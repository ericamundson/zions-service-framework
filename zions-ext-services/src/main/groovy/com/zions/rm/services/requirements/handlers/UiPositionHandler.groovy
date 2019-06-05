package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class UiPositionHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'UI Tab Name'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return val
	}

}
