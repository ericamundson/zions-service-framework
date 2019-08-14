package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class TableFileNameHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'Table/File Name'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return val
	}

}
