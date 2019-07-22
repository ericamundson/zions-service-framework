package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class FieldExistsHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'Field Exists'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return val
	}

}
