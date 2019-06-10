package com.zions.rm.services.requirements.handlers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.zions.rm.services.requirements.ClmRequirementsManagementService

@Component
class CreatorHandler extends RmBaseAttributeHandler {
	@Autowired
	ClmRequirementsManagementService clmRequirementsManagementService
	
	@Override
	public String getFieldName() {
		
		return 'creator'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		String outVal = null
		String ownerUrl = value
		if (ownerUrl == null || ownerUrl.length() == 0) return null
		String email = clmRequirementsManagementService.getMemberEmail(ownerUrl)
		outVal = email.toLowerCase()
		return outVal;
	}

}
