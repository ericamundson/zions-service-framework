package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class UiTransNumHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'UI Transaction Number'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return val
	}

}
