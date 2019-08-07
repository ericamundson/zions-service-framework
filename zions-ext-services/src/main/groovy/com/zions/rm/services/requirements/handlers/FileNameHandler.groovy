package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class FileNameHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'File Name'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return val
	}

}
