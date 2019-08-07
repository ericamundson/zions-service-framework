package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class ScreenFieldTypeHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'Screen Field Type'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return val
	}

}
