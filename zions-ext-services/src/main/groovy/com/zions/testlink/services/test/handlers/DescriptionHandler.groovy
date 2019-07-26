package com.zions.testlink.services.test.handlers

import org.springframework.stereotype.Component

@Component('TlDescriptionHandler')
class DescriptionHandler extends TlBaseAttributeHandler {

	public String getFieldName() {
		return 'notes'
	}

	public def formatValue(def value, def data) {
		String outVal = "${value}"
		if (value == null || value.length() == 0) return null
		return outVal;
	}


}