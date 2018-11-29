package com.zions.qm.services.test.handlers

import org.springframework.stereotype.Component

@Component
class ResultStartDateHandler extends QmBaseAttributeHandler {
	static int SIZE = 255

	public String getQmFieldName() {
		// TODO Auto-generated method stub
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
