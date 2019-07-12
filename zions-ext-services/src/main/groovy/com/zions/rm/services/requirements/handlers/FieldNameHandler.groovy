package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class FieldNameHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'Field Name'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return val
	}

}
