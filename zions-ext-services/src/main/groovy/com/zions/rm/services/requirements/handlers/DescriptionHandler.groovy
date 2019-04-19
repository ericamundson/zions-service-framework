package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component
import com.zions.common.services.attachments.IAttachments
import com.zions.common.services.cache.ICacheManagementService
import com.zions.rm.services.requirements.ClmArtifact
import com.zions.rm.services.requirements.ClmRequirementsFileManagementService
import com.zions.rm.services.requirements.ClmRequirementsManagementService
import com.zions.rm.services.requirements.RequirementsMappingManagementService

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

import groovy.util.logging.Slf4j
import groovy.xml.XmlUtil

@Component
@Slf4j
class DescriptionHandler extends RmBaseAttributeHandler {

	@Autowired
	ClmRequirementsManagementService clmRequirementsManagementService
	
	@Autowired
	IAttachments attachmentService
	
	@Autowired
	ICacheManagementService cacheManagementService
	
	@Autowired
	RequirementsMappingManagementService rmMappingManagementService
	
	@Autowired	
	ClmRequirementsFileManagementService rmFileManagementService
	
	@Autowired
	@Value('${clm.url}')
	String clmUrl
	
	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Primary Text'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		if (value == null || value.length() == 0) {
			return '<div></div>'
		}
		
		String sId = itemData.getCacheID()
		String outHtml
		if (itemData.getFormat() == 'WrapperResource') {
			// For wrapper resource (uploaded file), we need to create our own description with hyperlink to attachment
			def fileItem = rmFileManagementService.ensureRequirementFileAttachment(itemData, itemData.getFileHref(),'')

			outHtml = '<div><a href=' + fileItem.url + '&download=true>Uploaded Attachment</a></div>'
		}
		else {
			// strip out all namespace stuff from html
			String description = "${value}".replace("h:div xmlns:h='http://www.w3.org/1999/xhtml'",'div').replace('<h:','<').replace('</h:','</')
			description = description.replace('div xmlns="http://www.w3.org/1999/xhtml"','div')
			// Process any embedded images

			outHtml = processHtml(description, sId, itemData)
		}
		return 	outHtml
	}
	
	def processHtml(String html, String sId, def itemData) {
		def htmlData
		try {
			htmlData = new XmlSlurper().parseText(html)
		}
		catch (Exception e) {
			log.error("Error parsing description for ID &sId: ${e.getMessage()}")
			return null
		}
		// First move all embedded images to ADO
		def imgs = htmlData.'**'.findAll { p ->
			String src = p.@src
			"${p.name()}" == 'img' && "${src}".startsWith(this.clmUrl)
		}
		imgs.each { img ->
			String url = img.@src
			String altFilename = "${img.@alt}".replace(' WrapperResource','') + '.jpeg'
			def fileItem = rmFileManagementService.ensureRequirementFileAttachment(itemData, url, altFilename)
			img.@src = fileItem.url
			
			// If the embedded image was due to an embedded wrapper resource artifact, we want to get the original document attachment
			int wrapNdx = url.indexOf('resourceRevisionURL')
			if (wrapNdx > 0) {
				// Need to pull in the attachment for the embedded wrapped resource
				def about = clmUrl + '/rm/resources/' + url.substring(wrapNdx+74)
				def wrappedResourceArtifact = new ClmArtifact('','',about)
				wrappedResourceArtifact = clmRequirementsManagementService.getNonTextArtifact(wrappedResourceArtifact)
				fileItem = rmFileManagementService.ensureRequirementFileAttachment(itemData, wrappedResourceArtifact.getFileHref(),'')
			}
		}
		
		// Next process all tables, adding border info to <td> tags
		addBorderStyle('th', htmlData)
		addBorderStyle('td', htmlData)

		// Return html as string, but remove <?xml tag as it causes issues
		return XmlUtil.asString(htmlData).replace('<?xml version="1.0" encoding="UTF-8"?>\n', '')

	}
	
	def addBorderStyle(String tag, def htmlData) {
		def tds = htmlData.'**'.findAll { p ->
			"${p.name()}" == "${tag}"
		}
		tds.each { td ->
			String style = td.@style
			td.@style = style + ';border:1px solid black'
		}		
	}

}
