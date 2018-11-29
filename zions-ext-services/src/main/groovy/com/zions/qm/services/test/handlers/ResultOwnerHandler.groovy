package com.zions.qm.services.test.handlers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.qm.services.test.ClmTestManagementService

@Component
class ResultOwnerHandler extends QmBaseAttributeHandler {
	@Autowired
	ClmTestManagementService clmTestManagementService

	public String getQmFieldName() {
		// TODO Auto-generated method stub
		return 'owner'
	}

	public def formatValue(def value, def data) {
		String outVal = "${value}"
		if (value.length() > SIZE) {
			outVal = value.substring(0, SIZE-1)
		}
		return outVal;
	}

}
