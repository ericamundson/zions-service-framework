package com.zions.jama.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class ReasonHandler extends RmBaseAttributeHandler {
	
	@Override
	public String getFieldName() {
		
		return 'Requirement Status'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		// Handle cleanup via the DELETE IN ADO Release value - set status to Closed
		if (value == 'Draft') {
			return ''
		}
		else {  // Reason should be set to legacy status
			return value
		}
	}


}
