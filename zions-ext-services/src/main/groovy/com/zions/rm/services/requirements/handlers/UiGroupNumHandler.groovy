package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class UiGroupNumHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'UI Field Group Number'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return val
	}

}
