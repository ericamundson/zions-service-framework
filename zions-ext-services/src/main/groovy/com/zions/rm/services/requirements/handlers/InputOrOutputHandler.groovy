package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class InputOrOutputHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'ISZ Input or Output'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return val
	}

}
