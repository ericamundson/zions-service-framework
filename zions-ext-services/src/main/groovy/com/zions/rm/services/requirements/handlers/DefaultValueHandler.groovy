package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class DefaultValueHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'Default Value'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return val
	}

}
