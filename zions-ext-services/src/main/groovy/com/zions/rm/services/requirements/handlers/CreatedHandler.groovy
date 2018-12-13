package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class CreatedHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'created'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		String outVal = "${value}"
		if (value.length() == 0) {
			return null
		}
		return outVal;
	}

}
