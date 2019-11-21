package com.zions.testlink.services.test.handlers

import org.apache.commons.lang.StringEscapeUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import br.eti.kinoshita.testlinkjavaapi.model.TestCase
import br.eti.kinoshita.testlinkjavaapi.model.TestCaseStep
import com.zions.common.services.attachments.IAttachments
import com.zions.common.services.cache.ICacheManagementService
import com.zions.qm.services.test.ClmTestManagementService
import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil

@Component('TlStepsHandler')
class StepsHandler extends TlBaseAttributeHandler {
	
	@Autowired
	IAttachments attachmentService
	
	@Autowired
	ICacheManagementService cacheManagementService
	
	
	def SPECIAL = ['&nbsp;','&copy;','&reg;','&euro;','&trade;','&larr;','&uarr;','&rarr;','&darr;']

	public String getFieldName() {
		
		return 'none'
	}

	public def formatValue(def value, def data) {
		TestCase itemData = data.itemData
		def id = data.id
		String outVal = null
		outVal = buildStepData(itemData, id)
		return outVal;
	}
	
	String buildStepData(TestCase tc, id) {
		List<TestCaseStep> teststeps = tc.getSteps()
		if (teststeps.size()> 0) {
			List<TestCaseStep> steststeps = teststeps.sort() { it.number }
			def writer = new StringWriter()
			MarkupBuilder stepxml = new MarkupBuilder(new IndentPrinter(new PrintWriter(writer), "", false))
			int sCount = 2
			stepxml.steps(steps: 0, last: teststeps.size()+1) {
				steststeps.each { TestCaseStep astep ->
					step(id: sCount, type:'ValidateStep') {
						if (astep.actions.length() > 0) {
							String aStr = "${astep.actions}"
							aStr = aStr.replaceAll(/<!--[\s\S]*?-->/) {
								return ''
							}
							String htmlDoc = "<DIV><P>${aStr}</P></DIV>"
							
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
						if (astep.expectedResults.length() > 0) {
							String aStr = "${astep.expectedResults}"
							aStr = aStr.replaceAll(/<!--[\s\S]*?-->/) {
								return ''
							}
							String htmlDoc = "<DIV><P>${aStr}</P></DIV>"
							//String htmlDoc = "<DIV><P>${astep.expectedResults}</P></DIV>"
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
	
//	String processImages(String html, String sId) {
//		def htmlData = new XmlSlurper().parseText(html)
//		def imgs = htmlData.'**'.findAll { p ->
//			String src = p.@src
//			"${p.name()}" == 'img' && "${src}".startsWith(this.clmUrl)
//		}
//		imgs.each { img ->
//			String url = img.@src
//			def oData = clmTestManagementService.getContent(url)
//			def file = cacheManagementService.saveBinaryAsAttachment(oData.data, oData.filename, sId)
//			def attData = attachmentService.sendAttachment([file:file])
//			img.@src = attData.url
//		}
//		String outHtml = XmlUtil.asString(htmlData)
//		return outHtml
//	}


}
