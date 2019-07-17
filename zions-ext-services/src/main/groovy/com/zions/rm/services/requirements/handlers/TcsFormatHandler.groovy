package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class TcsFormatHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'TCS Format'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		return val
	}

}
