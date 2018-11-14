package com.zions.qm.services.test.handlers

import org.springframework.stereotype.Component

@Component
class NameHandler extends QmBaseAttributeHandler {
	static int SIZE = 255

	public String getQmFieldName() {
		// TODO Auto-generated method stub
		return 'title'
	}

	public String formatValue(String value, def fieldData) {
		String outVal = value
		if (value.length > SIZE) {
			outVal = value.substring(0, SIZE-1)
		}
		return outVal;
	}

}
