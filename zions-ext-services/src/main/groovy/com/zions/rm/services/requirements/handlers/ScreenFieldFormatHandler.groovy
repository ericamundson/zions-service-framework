package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class ScreenFieldFormatHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'Screen Field Format'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return val
	}

}
