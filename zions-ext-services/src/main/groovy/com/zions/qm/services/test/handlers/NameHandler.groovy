package com.zions.qm.services.test.handlers

import org.springframework.stereotype.Component

@Component('QmNameHandler')
class NameHandler extends QmBaseAttributeHandler {
	static int SIZE = 255

	public String getQmFieldName() {
		
		return 'title'
	}

	public def formatValue(def value, def data) {
		String outVal = "${value}"
		if (value.length() > SIZE) {
			outVal = value.substring(0, SIZE-1)
		}
		return outVal;
	}

}
