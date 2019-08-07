package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class RecordTypeHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'Record Type'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return val
	}

}
