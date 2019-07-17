package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class ValidValuesHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'ValidValues'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return toHtml(val)
	}

}
