package com.zions.testlink.services.test.handlers

import org.springframework.stereotype.Component

@Component('TlNameHandler')
class NameHandler extends TlBaseAttributeHandler {
	static int SIZE = 255

	public String getFieldName() {
		
		return 'summary'
	}

	public def formatValue(def value, def data) {
		String outVal = "${value}"
		if (value.length() > SIZE) {
			outVal = value.substring(0, SIZE-1)
		}
		return outVal;
	}

}
