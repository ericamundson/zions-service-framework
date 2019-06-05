package com.zions.qm.services.test.handlers

import com.zions.qm.services.test.ClmTestManagementService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component('QmStateHandler')
class StateHandler extends QmBaseAttributeHandler {
	@Autowired
	ClmTestManagementService clmTestManagementService
	
	public String getQmFieldName() {
		
		return 'state'
	}

	public def formatValue(def value, def data) {
		def itemData = data.itemData
		String stateUrl = "${itemData.state.@'ns7:resource'}"
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

}
