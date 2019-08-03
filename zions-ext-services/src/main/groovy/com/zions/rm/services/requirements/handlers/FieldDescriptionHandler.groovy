package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class FieldDescriptionHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'Field Description'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return toHtml(val)
	}

}
