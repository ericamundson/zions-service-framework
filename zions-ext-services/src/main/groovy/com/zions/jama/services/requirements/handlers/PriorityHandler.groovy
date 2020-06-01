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
		if (value == 165492) {
			return 3
		}
		else if (value == 165493) {
			return 2
		}
		else if (value == 165494) {
			return 1
		}
		else {
			return null
		}
	}

}
