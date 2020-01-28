package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class LengthHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Length'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		if (val) {
		val = val.replaceAll("[^\\d.]", "")
		}
		return stringToNumber(val, 'LengthHandler')
	}

}
