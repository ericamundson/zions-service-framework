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

@Component('QmStepsHandler')
class StepsHandler extends QmBaseAttributeHandler {
	@Autowired
	ClmTestManagementService clmTestManagementService
	
	@Autowired
	IAttachments attachmentService
	
	@Autowired
	ICacheManagementService cacheManagementService
	
	@Autowired
	@Value('clm.url')
	String clmUrl
	
	def SPECIAL = ['&nbsp;','&copy;','&reg;','&euro;','&trade;','&larr;','&uarr;','&rarr;','&darr;']

	public String getQmFieldName() {
		
		return 'none'
	}

	public def formatValue(def value, def data) {
		def itemData = data.itemData
		def id = data.id
		String outVal = null
		def ts = getTestScript(itemData)
		if (ts != null) 
		{
			outVal = buildStepData(ts, id)
		}
		return outVal;
	}
	
	String buildStepData(ts, id) {
		def teststeps = ts.steps.step
		if (teststeps.size()> 0) {
			def writer = new StringWriter()
			MarkupBuilder stepxml = new MarkupBuilder(new IndentPrinter(new PrintWriter(writer), "", false))
			int sCount = 2
			stepxml.steps(steps: 0, last: teststeps.size()+1) {
				teststeps.each { astep ->
					step(id: sCount, type:'ValidateStep') {
						if (astep.description.div.size() > 0) {
							String htmlDoc = XmlUtil.serialize( astep.description.div )
							//CharMatcher m
							//htmlDoc = htmlDoc.replaceAll("\\p{Cntrl}", "")
							htmlDoc = htmlDoc.replace('tag0:', '')
							htmlDoc = htmlDoc.replace(' xmlns:tag0="http://www.w3.org/1999/xhtml"', '')
							htmlDoc = htmlDoc.replace(' dir="ltr"', '')
							//htmlDoc = processImages(htmlDoc, id)
							//htmlDoc = removeHyperlinks(htmlDoc)
							htmlDoc = htmlDoc.replace('<?xml version="1.0" encoding="UTF-8"?>', '')
							//htmlDoc = htmlDoc.replaceAll(/&+(\w)+;/,'')
							//htmlDoc = htmlDoc.replace('&copy;','')
							//String plainText= Jsoup.parse(htmlDoc).text();
//							if (htmlDoc.length() == 0) {
//								htmlDoc = 'n/a'
//							}
							htmlDoc = "<DIV><P>${htmlDoc}</P></DIV>"
//							File action = new File('action.html')
//							def os = action.newDataOutputStream()
//							os << htmlDoc
//							os.close()
							
							String html = StringEscapeUtils.escapeHtml(htmlDoc)
							parameterizedString(isformatted: 'true') {
								mkp.yieldUnescaped html
							}
						} else {
							String html = StringEscapeUtils.escapeHtml('<DIV><P></P></DIV>')
							parameterizedString(isformatted: 'true') {
								mkp.yieldUnescaped html
							}
						}
						if (astep.expectedResult.div.size() > 0) {
							String htmlDoc = XmlUtil.serialize( astep.expectedResult.div )
							//htmlDoc = htmlDoc.replaceAll("\\p{Cntrl}", "")
							htmlDoc = htmlDoc.replace('tag0:', '')
							htmlDoc = htmlDoc.replace(' xmlns:tag0="http://www.w3.org/1999/xhtml"', '')
							htmlDoc = htmlDoc.replace(' dir="ltr"', '')
//							htmlDoc = htmlDoc.replaceAll(/&+(\w)+;/,'')
							//htmlDoc = processImages(htmlDoc, id)
							//htmlDoc = removeHyperlinks(htmlDoc)
							htmlDoc = htmlDoc.replace('<?xml version="1.0" encoding="UTF-8"?>', '')
							//String plainText= Jsoup.parse(htmlDoc).text();
//							if (htmlDoc.length() == 0) {
//								htmlDoc = 'n/a'
//							}
							htmlDoc = "<DIV><P>${htmlDoc}</P></DIV>"
//							File action = new File('result.html')
//							def os = action.newDataOutputStream()
//							os << htmlDoc
//							os.close()
							String html = StringEscapeUtils.escapeHtml(htmlDoc)
							parameterizedString(isformatted: 'true') {
								mkp.yieldUnescaped html
							}
						} else {
							String html = StringEscapeUtils.escapeHtml('<DIV><P></P></DIV>')
							parameterizedString(isformatted: 'true') {
								mkp.yieldUnescaped html
							}

						}
						//description()
					}
					sCount++
				}
			}
			String outVal = writer.toString()
			outVal = outVal.replaceAll("\\p{Cntrl}", "")
			outVal = clearSpecial(outVal)
//			outVal = outVal.replace('&nbsp;','')
//			outVal = outVal.replace('&copy;','')
			//outVal = outVal.replaceAll("\\s","")
			return outVal
		}
		return null
	}
	
	def clearSpecial(String val) {
		String out = val
		SPECIAL.each { sym ->
			out = out.replace(sym,'')
		}
		return out
	}
	
	private def getTestScript(def itemData) {
		def tss = itemData.testscript
		if (tss.size() > 0) {
			String href = "${tss[0].@href}"
			def ts = clmTestManagementService.getTestItem(href)
			return ts
		}
		return null
	}
	
	String removeHyperlinks(String html) {
		def htmlData = new XmlSlurper().parseText(html)
		def links = htmlData.'**'.findAll { p ->
			String href = p.@href
			"${href}".startsWith('http')
		}
		links.each { node ->
			String href = node.@href
			node.replaceNode {
				b("${href}")
			}
		}
		String outHtml = XmlUtil.asString(htmlData)
		return outHtml


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
