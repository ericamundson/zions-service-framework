package com.zions.testlink.services.test.handlers

import org.springframework.stereotype.Component

@Component('TlResultStartDateHandler')
class ResultStartDateHandler extends TlBaseAttributeHandler {
	static int SIZE = 255

	public String getFieldName() {
		
		return 'starttime'
	}

	public def formatValue(def value, def data) {
		String outVal = "${value}"
		if (value.length() == 0) {
			return null
		}
		return outVal;
	}

}
