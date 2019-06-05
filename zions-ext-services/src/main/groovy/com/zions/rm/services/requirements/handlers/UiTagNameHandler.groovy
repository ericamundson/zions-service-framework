package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class UiTagNameHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'UI Tag Name'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return val
	}

}
