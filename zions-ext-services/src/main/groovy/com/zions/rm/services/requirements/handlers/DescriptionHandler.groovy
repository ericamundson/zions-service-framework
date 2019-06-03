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
		String sId = itemData.getCacheID()
		String outHtml
		if (itemData.getArtifactType() == 'Report Filter') {
			String seq = itemData.getAttribute('Sequence No')
			String filterDesc = itemData.getAttribute('Filter Description')
			outHtml = """<div><p><b>Sequence No:</b>&nbsp$seq</p>
                         <p><b>Filter Description:</b>&nbsp$filterDesc</p>"""
		}
		else if (itemData.getArtifactType() == 'Report Group Layout') {
			String section = itemData.getAttribute('Sections')
			String alignment = itemData.getAttribute('Alignment')
			String pageBreak = itemData.getAttribute('Page Break')
			outHtml = """<div><p><b>Section:</b>&nbsp$section</p>
                         <p><b>Alignment:</b>&nbsp$alignment</p>
                         <p><b>Page Break:</b>&nbsp$pageBreak</p>
                         <p><b>Value:</b>&nbsp${itemData.stripTags(value)}</p>"""
		}		
		else if (itemData.getArtifactType() == 'Report Sort') {
			String seq = itemData.getAttribute('Sequence No')
			String groupClause = itemData.getAttribute('Group Clause')
			String fieldName = itemData.getAttribute('Field Name')
			String gsb = itemData.getAttribute('Group or Sort')
			String order = itemData.getAttribute('Sort Order')
			String remarks = itemData.getAttribute('Info Comments')
			outHtml = """<div><p><b>Sequence No:</b>&nbsp$seq</p>
                         <p><b>Group Clause:</b>&nbsp$groupClause</p>
                         <p><b>Report Field Name:</b>&nbsp$fieldName</p>
                         <p><b>Group / Sort / Break:</b>&nbsp$gsb</p>
                         <p><b>Sort Order:</b>&nbsp$order</p>
                         <p><b>Remarks: </b>&nbsp$remarks</p></div>"""
		}
		else if (itemData.getArtifactType() == 'Report Summary Fields') {
			String field = itemData.getAttribute('Field Name')
			String valFormat = itemData.getAttribute('Value Format')
			String alignment = itemData.getAttribute('Alignment')
			String calc = itemData.getAttribute('Calculation Needed - Y/N')
			outHtml = """<div><p><b>Report Field Name:</b>&nbsp$field</p>
                         <p><b>Value Format:</b>&nbsp$valFormat</p>
                         <p><b>Data Alignment:</b>&nbsp$alignment</p>
                         <p><b>Field Mapping and Calculation:</b>&nbsp$calc</p>"""
		}
		else if (itemData.getArtifactType() == 'Reporting RRZ') {
			String title = itemData.getAttribute('Report Title')
			String pageSize = itemData.getAttribute('Page Size')
			String pageLayout = itemData.getAttribute('Page Layout')
			String docName = itemData.getAttribute('Document Name')
			String docNum = itemData.getAttribute('FS Document #')
			String audience = itemData.getAttribute('Audience')
			String distSchedule = itemData.getAttribute('Distribution Schedule')
			String distFormat = itemData.getAttribute('Distribution Format')
			String distMethod = itemData.getAttribute('Non-CORE System')
			String desc = itemData.getAttribute('Info Comments')
			String affiliates = itemData.getAttribute('Affiliates Affected')
			outHtml = """<div><p><b>Report Title:</b><br>
                         &nbsp&nbsp$title</p>
                         <p><b>Business Requirements:</b><br>
                         &nbsp&nbsp$docName<br>
                         &nbsp&nbsp$docNum</p>
                         <p><b>Description:</b><br>
                         &nbsp&nbsp$desc</p>
                         <p><b>Audience:</b><br>
                         &nbsp&nbsp${audience.replaceAll(';',', ')}</p>
                         <p><b>Distribution:</b><br>
						 &nbsp&nbspDistribution Schedule:&nbsp$distSchedule<br>
						 &nbsp&nbspDistribution Format:&nbsp$distFormat<br>
						 &nbsp&nbspDistribution Method:&nbsp${distMethod.replaceAll(';',', ')}</p>
                         <p><b>General Report and Page Layout:</b><br>
						 &nbsp&nbspPage Size:&nbsp$pageSize<br>
						 &nbsp&nbspPage Orientation:&nbsp$pageLayout</p></div>
                         <p><b>Input Parameters:</b><br>
                         &nbsp&nbspAffiliate:&nbsp${affiliates.replaceAll(';',', ')}</p>"""
		}
		else if (itemData.getFormat() == 'WrapperResource') {
			// For wrapper resource (uploaded file), we need to create our own description with hyperlink to attachment
			def fileItem = rmFileManagementService.ensureRequirementFileAttachment(itemData, itemData.getFileHref())
			if (fileItem) {
				if (isImageFile("${fileItem.fileName}")) {
					// convert uploaded image to embedded image
					outHtml = "<div><img alt='Embedded image' src='" + fileItem.url + "'/></div>"
				}
				else {
					// include link to attached document
					outHtml = "<div><a href='" + fileItem.url + "&amp;download=true'>Uploaded Attachment: ${fileItem.fileName}</a></div>"
				}
			} else {
				outHtml = "<div>Uploading Attachment from CLM failed, please see original work item</div>"
			}
		}
		else if (value == null || value.length() == 0) {
			return '<div></div>'
		}		
		else {
			// strip out all namespace stuff from html
			String description = "${value}".replace("h:div xmlns:h='http://www.w3.org/1999/xhtml'",'div').replace('<h:','<').replace('</h:','</')
			description = description.replace('div xmlns="http://www.w3.org/1999/xhtml"','div')

			// Process any embedded images and table formatting
			outHtml = processHtml(description, sId, itemData)
		}
		outHtml = outHtml.replaceAll("&lt;",'<').replaceAll("&gt;",'>').replaceAll("[^\\p{ASCII}]", "")

		return outHtml
	}
	
	def processHtml(String html, String sId, def itemData) {
		def htmlData
		try {
			htmlData = new XmlSlurper().parseText(html)
		}
		catch (Exception e) {
			log.error("Error parsing description for ID $sId: ${e.getMessage()}")
			return null
		}
		// First move all embedded images or embedded attachments to ADO
		def wrapperRootNode
		def imgs = htmlData.'**'.findAll { p ->
			String src = p.@src
			"${p.name()}" == 'img' && "${src}".startsWith(this.clmUrl)
		}
		imgs.each { img ->
			String url = img.@src
			def fileItem
			// If the embedded image was due to an embedded wrapper resource artifact, we want to get the original document attachment
			int wrapNdx = url.indexOf('resourceRevisionURL')
			if (wrapNdx > 0) {
				// Need to pull in the attachment for the embedded wrapped resource
				def about = clmUrl + '/rm/resources/' + url.substring(wrapNdx+74)
				def wrappedResourceArtifact = new ClmArtifact('','',about)
				wrappedResourceArtifact = clmRequirementsManagementService.getNonTextArtifact(wrappedResourceArtifact, false)
				fileItem = rmFileManagementService.ensureRequirementFileAttachment(itemData, wrappedResourceArtifact.getFileHref())
				
				// Now delete image node
				String attachmentLink
				if (isImageFile("${fileItem.fileName}")) {
					// Convert uploaded image to an embedded image
					attachmentLink = "<div><img alt='Embedded image' src='" + fileItem.url + "'/></div>"
				}
				else {
					attachmentLink = "<div><a href='" + fileItem.url + "&amp;download=true'>Uploaded Attachment: ${fileItem.fileName}</a></div>"
				}
				if (wrapperRootNode) {
					wrapperRootNode.appendNode(new XmlSlurper().parseText(attachmentLink))
				}
				else {
					wrapperRootNode = new XmlSlurper().parseText(attachmentLink)
				}
			}
			else {
				fileItem = rmFileManagementService.ensureRequirementFileAttachment(itemData, url)
				if(fileItem) {
				img.@src = fileItem.url		
				} else {
					log.error("Error uploading attachment for ID ${sId}")
				}
			}
			

		}
		// If there are any embedded documents, we just return the wrapper document links
		if (wrapperRootNode) {
			htmlData = wrapperRootNode
		}	
		else {	
			// Next process all tables, adding border info to <td> tags
			addBorderStyle('th', htmlData)
			addBorderStyle('td', htmlData)
		}

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

	boolean isImageFile(String filename) {
		return (filename.toLowerCase().indexOf('.png') > 0 || filename.toLowerCase().indexOf('.jpg') > 0)
	}
}
