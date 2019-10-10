package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class TcsLengthHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'TCS Length'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		return stringToNumber(val, 'TcsLengthHandler')
	}

}
