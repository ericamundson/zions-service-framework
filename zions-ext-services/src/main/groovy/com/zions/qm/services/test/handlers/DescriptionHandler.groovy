package com.zions.qm.services.test.handlers

import org.springframework.stereotype.Component

@Component
class DescriptionHandler extends QmBaseAttributeHandler {

	public String getQmFieldName() {
		// TODO Auto-generated method stub
		return 'description'
	}

	public String formatValue(String value, def fieldData) {
		String outVal = value
		if (value == null || value.length() == 0) return null
		return outVal;
	}

}
