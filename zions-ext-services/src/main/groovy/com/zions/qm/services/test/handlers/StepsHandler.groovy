package com.zions.qm.services.test.handlers

import org.apache.commons.lang.StringEscapeUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.qm.services.test.ClmTestManagementService
import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil

@Component
class StepsHandler extends QmBaseAttributeHandler {
	@Autowired
	ClmTestManagementService clmTestManagementService

	public String getQmFieldName() {
		// TODO Auto-generated method stub
		return 'none'
	}

	public def formatValue(def value, def itemData) {
		String outVal = null
		def ts = getTestScript(itemData)
		if (ts != null) 
		{
			outVal = buildStepData(ts)
		}
		return outVal;
	}
	
	String buildStepData(ts) {
		def teststeps = ts.steps.step
		if (teststeps.size()> 0) {
			def writer = new StringWriter()
			MarkupBuilder stepxml = new MarkupBuilder(writer)
			int sCount = 2
			stepxml.steps(steps: 0, last: teststeps.size()+1) {
				teststeps.each { astep ->
					step(id: sCount, type:'ValidateStep') {
						if (astep.description.div.size() > 0) {
							String htmlDoc = XmlUtil.serialize( astep.description.div )
							htmlDoc = htmlDoc.replace('<?xml version="1.0" encoding="UTF-8"?>', '')
							htmlDoc = htmlDoc.replace('tag0:', '')
							htmlDoc = htmlDoc.replace(' xmlns:tag0="http://www.w3.org/1999/xhtml"', '')
							htmlDoc = htmlDoc.replace(' dir="ltr"', '')
							String html = StringEscapeUtils.escapeHtml(htmlDoc)
							parameterizedString(isFormatted: 'true') {
								mkp.yieldUnescaped html
							}
						}
						if (astep.expectedResult.div.size() > 0) {
							String htmlDoc = XmlUtil.serialize( astep.expectedResult.div )
							htmlDoc = htmlDoc.replace('<?xml version="1.0" encoding="UTF-8"?>', '')
							htmlDoc = htmlDoc.replace('tag0:', '')
							htmlDoc = htmlDoc.replace(' xmlns:tag0="http://www.w3.org/1999/xhtml"', '')
							htmlDoc = htmlDoc.replace(' dir="ltr"', '')
							String html = StringEscapeUtils.escapeHtml(htmlDoc)
							parameterizedString(isFormatted: 'true') {
								mkp.yieldUnescaped html
							}
						}
					}
					sCount++
				}
			}
			String outVal = writer.toString()
			return outVal
		}
		return null
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

}
