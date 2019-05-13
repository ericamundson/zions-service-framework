package com.zions.qm.services.test.handlers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.qm.services.test.ClmTestManagementService

@Component('QmResultOwnerHandler')
class ResultOwnerHandler extends QmBaseAttributeHandler {
	@Autowired
	ClmTestManagementService clmTestManagementService

	public String getQmFieldName() {
		// TODO Auto-generated method stub
		return 'owner'
	}

	public def formatValue(def value, def data) {
		def outVal = null
		def itemData = data.itemData
		String url = "${itemData.owner.@'ns7:resource'}"
		def owner = clmTestManagementService.getTestItem(url)
		if (owner != null ) {
			String email = "${owner.emailAddress.text()}"
			outVal = email.toLowerCase()
		}
		return outVal;
	}

}
