package com.zions.qm.services.test.handlers

import com.zions.qm.services.test.ClmTestManagementService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class OwnerHandler extends QmBaseAttributeHandler {
	@Autowired
	ClmTestManagementService clmTestManagementService
	
	public String getQmFieldName() {
		// TODO Auto-generated method stub
		return 'owner'
	}

	public def formatValue(def value, def data) {
		def itemData = data.itemData
		String ownerUrl = "${itemData.owner.@'ns7:resource'}"
		if (ownerUrl == null || ownerUrl.length() == 0) return null
		def ownerInfo = clmTestManagementService.getTestItem(ownerUrl)
		String outVal = "${ownerInfo.emailAddress.text()}"
		return outVal;
	}

}
