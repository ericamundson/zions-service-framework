package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class IszFieldTypeHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'ISZ Field Type'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return val
	}

}
