package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class UiValidationHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'UI Validation'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return toHtml(val)
	}

}
