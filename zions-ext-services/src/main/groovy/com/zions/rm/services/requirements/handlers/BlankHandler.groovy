package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class BlankHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return null
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return null
	}

}
