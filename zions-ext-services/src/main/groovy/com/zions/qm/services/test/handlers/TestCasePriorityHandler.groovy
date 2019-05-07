package com.zions.qm.services.test.handlers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.qm.services.test.ClmTestManagementService

@Component('QmTestCasePriorityHandler')
class TestCasePriorityHandler extends QmBaseIntegerAttributeHandler {
	
	@Autowired
	ClmTestManagementService clmTestManagementService
	
	def priorities = null


	public String getQmFieldName() {
		// TODO Auto-generated method stub
		return 'priority'
	}

	public def formatValue(def value, def data) {
		def itemData = data.itemData
		String name = "${value}"
		def priority = itemData.priority
		if (priority.size()>0) {
			String url = priority.'@ns7:resource'
			def ps = getPriorities(url)
			String val = ps.find { node ->
				String id = "${node.identifier.text()}"
				id == name
			}.title.text()
//			if (val == 'Unassigned') {
//				return null
//			}
			return val
		}
		return null
	}
	
	def getPriorities(String url) {
		if (priorities && priorities.size() > 0) return priorities
		def priorityStuff = clmTestManagementService.getTestItem(url)
		priorities = priorityStuff.'**'.findAll { node ->
			node.name() == 'Priority'
		}
		return priorities
	}
	
	

}
