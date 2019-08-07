package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class DefaultValuesHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'Default Values'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return val
	}

}
