package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component
import com.zions.common.services.attachments.IAttachments
import com.zions.common.services.cache.ICacheManagementService
import com.zions.rm.services.requirements.ClmRequirementsManagementService

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
	@Value('${clm.url}')
	String clmUrl
	
	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Primary Text'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		if (value == null || value.length() == 0) return null
		
		// strip out all namespace stuff from html
		String description = "${value}".replace("h:div xmlns:h='http://www.w3.org/1999/xhtml'",'div').replace('<h:','<').replace('</h:','</')
		description = description.replace('div xmlns="http://www.w3.org/1999/xhtml"','div')
		// Process any embedded images
		String sId = itemData.getCacheID()
		def outHtml = processHtml(description, sId)
		
		// Return html string, but remove <?xml tag as it causes issues
		return XmlUtil.asString(outHtml).replace('<?xml version="1.0" encoding="UTF-8"?>\n', '')	
	}
	
	def processHtml(String html, String sId) {
		def htmlData = new XmlSlurper().parseText(html)
		
		// First move all embedded images to ADO
		def imgs = htmlData.'**'.findAll { p ->
			String src = p.@src
			"${p.name()}" == 'img' && "${src}".startsWith(this.clmUrl)
		}
		imgs.each { img ->
			String url = img.@src
			def oData = clmRequirementsManagementService.getContent(url)
			String contentDisp = "${oData.headers.'Content-Disposition'}"
			String filename = null
			if (contentDisp != null && contentDisp != 'null') {
				filename = contentDisp.substring(contentDisp.indexOf('filename=')+10,contentDisp.indexOf('";'))
			}
			else {
				filename = "${img.@alt}".replace(' WrapperResource','')
			}
			if (filename != null) {
				def file = cacheManagementService.saveBinaryAsAttachment(oData.data, filename, sId)
				def attData = attachmentService.sendAttachment([file:file])
				img.@src = attData.url
			}
			else {
				log.error("Error parsing filename of embedded image in DescriptionHandler.  Artifact ID: $itemId")
			}		
		}
		
		// Next process all tables, adding border info to <td> tags
		addBorderStyle('th', htmlData)
		addBorderStyle('td', htmlData)

		return htmlData

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
