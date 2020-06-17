package com.zions.jama.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class NameHandler extends RmBaseAttributeHandler {
	
	@Override
	public String getFieldName() {
		
		return 'name'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		return value
	}
}
