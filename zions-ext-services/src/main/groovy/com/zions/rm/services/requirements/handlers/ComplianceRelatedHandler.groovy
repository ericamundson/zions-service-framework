package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class ComplianceRelatedHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'Compliance Related'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		if (val == 'Yes') {
			val = 'COMPLIANCE'
		}
		else {
			val = ''
		}
		return val
	}

}
