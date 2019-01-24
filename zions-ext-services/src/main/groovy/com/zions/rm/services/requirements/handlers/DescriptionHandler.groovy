package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component
import com.zions.common.services.attachments.IAttachments
import com.zions.common.services.cache.ICacheManagementService
import com.zions.rm.services.requirements.ClmRequirementsManagementService

import org.springframework.beans.factory.annotation.Autowired
import groovy.xml.XmlUtil

@Component
class DescriptionHandler extends RmBaseAttributeHandler {

	@Autowired
	ClmRequirementsManagementService clmRequirementsManagementService
	
	@Autowired
	IAttachments attachmentService
	
	@Autowired
	ICacheManagementService cacheManagementService

	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Primary Text'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		if (value == null || value.length() == 0) return null
		
		// strip out all namespace stuff from html
		String description = "${value}".replace("<h:div xmlns:h='http://www.w3.org/1999/xhtml'>",'<div>').replace('<h:','<').replace('</h:','</')
		
		// Process any embedded images
		String sId = itemData.getID()
		return processImages(description, sId);
	}
	
	String processImages(String html, String sId) {
		def htmlData = new XmlSlurper().parseText(html)
		def imgs = htmlData.'**'.findAll { p ->
			String src = p.@src
			"${p.name()}" == 'img' && "${src}".startsWith(this.clmUrl)
		}
		imgs.each { img ->
			String url = img.@src
			def oData = clmRequirementsManagementService.getContent(url)
			def file = cacheManagementService.saveBinaryAsAttachment(oData.data, oData.filename, sId)
			def attData = attachmentService.sendAttachment([file:file])
			img.@src = attData.url
		}
		String outHtml = XmlUtil.asString(htmlData)
		return outHtml
	}

}
