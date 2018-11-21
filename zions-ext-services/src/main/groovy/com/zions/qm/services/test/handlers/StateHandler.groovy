package com.zions.qm.services.test.handlers

import com.zions.qm.services.test.ClmTestManagementService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class StateHandler extends QmBaseAttributeHandler {
	@Autowired
	ClmTestManagementService clmTestManagementService
	
	public String getQmFieldName() {
		// TODO Auto-generated method stub
		return 'state'
	}

	public String formatValue(String value, def itemData) {
		String stateUrl = "${itemData.state.@'ns7:resource'}"
		def stateInfo = clmTestManagementService.getTestItem(stateUrl)
		def stateItem = stateInfo.ProcessInfo.hasWorkflowState.WorkflowState.find { state ->
			"${state.@'rdf:about'}" == "${stateUrl}"
		}
		if (stateItem != null) {
			return  "${stateItem.title.text()}"
		}
		return null;
	}

}
