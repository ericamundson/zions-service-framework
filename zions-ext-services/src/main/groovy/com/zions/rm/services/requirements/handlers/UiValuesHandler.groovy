package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class UiValuesHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'UI Field Values'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return toHtml(val)
	}

}
