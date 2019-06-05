package com.zions.qm.services.test.handlers

import org.apache.commons.lang.StringEscapeUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.common.services.attachments.IAttachments
import com.zions.common.services.cache.ICacheManagementService
import com.zions.qm.services.test.ClmTestManagementService
import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil

class HtmlHandler extends QmBaseAttributeHandler {
	@Autowired
	ClmTestManagementService clmTestManagementService
	
	@Autowired
	IAttachments attachmentService
	
	@Autowired
	ICacheManagementService cacheManagementService

	public String getQmFieldName() {
		return ''
	}

	public def formatValue(def value, def data) {
		def itemData = data.itemData
		def id = data.id
		String name = getQmFieldName()
		def field = itemData."${name}"
		String outVal = null;
		if (field) {
			outVal = buildHtml(field, id)
		}
		return outVal;
	}
	
	String buildHtml(field, String id) {
		if (field.div.size() > 0) {
			String htmlDoc = XmlUtil.serialize( field.div )
			
			htmlDoc = htmlDoc.replace('tag0:', '')
			htmlDoc = htmlDoc.replace(' xmlns:tag0="http://www.w3.org/1999/xhtml"', '')
			htmlDoc = htmlDoc.replace(' dir="ltr"', '')
			htmlDoc = processImages(htmlDoc, id)
			htmlDoc = htmlDoc.replace('<?xml version="1.0" encoding="UTF-8"?>', '')
			String outString = new String(htmlDoc.bytes, 'utf-8')
			return outString
		}
		return null
	}

	String processImages(String html, String sId) {
		def htmlData = new XmlSlurper().parseText(html)
		def imgs = htmlData.'**'.findAll { p ->
			String src = p.@src
			"${p.name()}" == 'img' && "${src}".startsWith(this.clmUrl)
		}
		imgs.each { img ->
			String url = img.@src
			def oData = clmTestManagementService.getContent(url)
			def file = cacheManagementService.saveBinaryAsAttachment(oData.data, oData.filename, sId)
			def attData = attachmentService.sendAttachment([file:file])
			img.@src = attData.url
		}
		String outHtml = XmlUtil.asString(htmlData)
		return outHtml
	}


}
