package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class ClosedReasonHandler extends RmBaseAttributeHandler {
	
	@Override
	public String getFieldName() {
		
		return 'Status'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		if (value == 'Duplicate' || value == 'Delete' || value == 'Rejected' || value == 'Deprecated' ) {
			return value
		}
		else {
			return null
		}
	}


}
