package com.zions.qm.services.test.handlers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.qm.services.test.ClmTestManagementService

@Component('QmOBTestCaseTeamAreaHandler')
class OBTestCaseTeamAreaHandler extends QmBaseAttributeHandler {
	
	@Autowired
	ClmTestManagementService clmTestManagementService
	
	def priorities = null


	public String getQmFieldName() {
		
		return 'category'
	}

	public def formatValue(def value, def data) {
		def itemData = data.itemData
		String val = null
		itemData.category.each { cat -> 
			String name = "${cat.@term}".replace(' ', '_')
			if (name == 'Component') {
				val = "${cat.@value}"
				return
			}
		}
		if (val) {
			return val
		}
		return null
	}
		

}
