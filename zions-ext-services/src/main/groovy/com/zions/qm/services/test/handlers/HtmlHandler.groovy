package com.zions.qm.services.test.handlers

import org.apache.commons.lang.StringEscapeUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.zions.common.services.attachments.IAttachments
import com.zions.common.services.cache.ICacheManagementService
import com.zions.qm.services.test.ClmTestManagementService
import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil
import java.nio.charset.StandardCharsets

class HtmlHandler extends QmBaseAttributeHandler {
	@Autowired
	ClmTestManagementService clmTestManagementService
	
	@Autowired
	IAttachments attachmentService
	
	@Autowired
	ICacheManagementService cacheManagementService
	
	@Value('${clm.url:}')
	String clmUrl

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
			//StandardCharsets g
			//String outString = new String(htmlDoc.bytes, StandardCharsets.US_ASCII)
			//htmlDoc = new String(htmlDoc.getBytes('UTF-8'))
			//htmlDoc = htmlDoc.replace('Â', '')
			return htmlDoc
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
			if (!oData || !oData.filename) return
			String fName = cleanTextContent(oData.filename)
			ByteArrayInputStream s = oData.data
			byte[] file = s.bytes
			def attData = attachmentService.sendAttachment([file:file, fileName: fName])
			img.@src = attData.url
		}
		String outHtml = XmlUtil.asString(htmlData)
		return outHtml
	}
	
	private static String cleanTextContent(String text)
	{
		if (text.lastIndexOf('\\') > -1) {
			text = text.substring(text.lastIndexOf('\\')+1)
		}
		// strips off all non-ASCII characters
		text = text.replaceAll("[^\\x00-\\x7F]", "");
 
		// erases all the ASCII control characters
		text = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
		 
		// removes non-printable characters from Unicode
		text = text.replaceAll("\\p{C}", "");
 
		return text.trim();
	}



}
