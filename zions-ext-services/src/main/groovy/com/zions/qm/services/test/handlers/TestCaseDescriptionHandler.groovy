package com.zions.qm.services.test.handlers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.qm.services.test.ClmTestManagementService
import groovy.xml.MarkupBuilder

@Component('QmTestCaseDescriptionHandler')
class TestCaseDescriptionHandler extends QmBaseAttributeHandler {
	@Autowired
	ClmTestManagementService clmTestManagementService

	public String getQmFieldName() {
		
		return 'description'
	}

	public def formatValue(def value, def data) {
		def itemData = data.itemData
		String tcDescription = value
		def ts = getTestScript(itemData)
		String tsDesc = null
		if (ts != null) 
		{
			tsDesc = "${ts.description.text()}"
		}
		String outVal = buildHtml(tcDescription, tsDesc)
		return outVal;
	}
	
	private String buildHtml(String tcDescription, String tsDescription) {
		def writer = new StringWriter()
		MarkupBuilder bHtml = new MarkupBuilder(writer)
		
		if (tcDescription != null && tcDescription.length()> 0) {
			bHtml.div(style:'border:2px solid black') {
				div {
					bold("RQM Test Case Description:")
				}
				
				div { mkp.yieldUnescaped tcDescription }
			}
		}
		if (tsDescription != null && tsDescription.length() > 0) {
			bHtml.div(style:'border:2px solid black') {
				div {
					bold("RQM Test Script Description:")
				}
				
				div { mkp.yieldUnescaped tsDescription }
			}
		}
		String outVal = writer.toString()
		if (outVal.length() == 0) return null
		return outVal

	}
	
	private def getTestScript(def itemData) {
		def tss = itemData.testscript;
		if (tss.size() > 0) {
			String href = "${tss[0].@href}"
			def ts = clmTestManagementService.getTestItem(href)
			return ts
		}
		return null
	}

}
