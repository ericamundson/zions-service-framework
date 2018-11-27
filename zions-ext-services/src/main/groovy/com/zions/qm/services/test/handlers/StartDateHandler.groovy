package com.zions.qm.services.test.handlers

import org.springframework.stereotype.Component

@Component
class StartDateHandler extends QmBaseAttributeHandler {
	static int SIZE = 255

	public String getQmFieldName() {
		// TODO Auto-generated method stub
		return 'title'
	}

	public def formatValue(def value, def fieldData) {
		String outVal = "${value}"
		if (outVal.length > SIZE) {
			outVal = outVal.substring(0, SIZE-1)
		}
		return outVal;
	}

}
