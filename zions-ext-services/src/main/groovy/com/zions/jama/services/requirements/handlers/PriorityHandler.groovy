package com.zions.jama.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class PriorityHandler extends RmBaseAttributeHandler {
	
	@Override
	public String getFieldName() {
		
		return 'priority'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		if (value == 'Unassigned' || value == '') {
			return null
		}
		return value;
	}

}
