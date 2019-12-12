package com.zions.qm.services.test.handlers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.qm.services.test.ClmTestManagementService

@Component('QmResultOwnerHandler')
class ResultOwnerHandler extends QmBaseAttributeHandler {
	@Autowired
	ClmTestManagementService clmTestManagementService

	public String getQmFieldName() {
		
		return 'owner'
	}

	public def formatValue(def value, def data) {
		def outVal = null
		def itemData = data.itemData
		String url = "${itemData.owner.@'ns7:resource'}"
		def owner =  null
		try {
			owner = clmTestManagementService.getTestItem(url)
		} catch (e) {}
		if (owner != null ) {
			String email = "${owner.emailAddress.text()}"
			outVal = [ uniqueName: email ]
		}
		return outVal;
	}

}
