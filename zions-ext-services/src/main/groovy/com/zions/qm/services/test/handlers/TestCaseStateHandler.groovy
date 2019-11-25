package com.zions.qm.services.test.handlers

import com.zions.qm.services.test.ClmTestManagementService
import groovy.xml.XmlUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component('QmTestCaseStateHandler')
class TestCaseStateHandler extends QmBaseAttributeHandler {
	@Autowired
	ClmTestManagementService clmTestManagementService
	
	public String getQmFieldName() {
		
		return 'state'
	}

	public def formatValue(def value, def data) {
		def itemData = data.itemData
		def testScript = getTestScript(itemData)
		if (!testScript) return null
		//println new XmlUtil().serialize(testScript)
		String stateUrl = "${testScript.state.@'ns7:resource'}"
		def stateInfo = clmTestManagementService.getTestItem(stateUrl)
		def stateItem = stateInfo.ProcessInfo.hasWorkflowState.WorkflowState.find { state ->
			"${state.@'rdf:about'}" == "${stateUrl}"
		}
		if (stateItem != null) {
			String retVal = "${stateItem.title.text()}"
			return retVal
		}
		return null;
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
