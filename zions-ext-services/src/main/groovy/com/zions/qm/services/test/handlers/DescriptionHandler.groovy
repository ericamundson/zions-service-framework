package com.zions.qm.services.test.handlers

import org.springframework.stereotype.Component

@Component
class DescriptionHandler extends QmBaseAttributeHandler {

	public String getQmFieldName() {
		// TODO Auto-generated method stub
		return 'description'
	}

	public def formatValue(def value, def itemData) {
		String outVal = "${value}"
		if (value == null || value.length() == 0) return null
		return outVal;
	}

}
