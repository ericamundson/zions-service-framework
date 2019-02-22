package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class UiOriginalTagNameHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'UI Original Tag Name'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return val
	}

}
