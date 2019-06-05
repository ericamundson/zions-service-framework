package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class UiFieldDisplayHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'UI Field Display'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return val
	}

}
