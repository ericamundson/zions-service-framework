package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class DbFieldNameHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'DB Field Name'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return val
	}

}
