package com.zions.testlink.services.test.handlers

import org.springframework.stereotype.Component

@Component('TlTestCaseDescriptionHandler')
class TestCaseDescriptionHandler extends TlBaseAttributeHandler {

	public String getFieldName() {
		return 'summary'
	}

	public def formatValue(def value, def data) {
		String outVal = "${value}"
		if (value == null || value.length() == 0) return null
		return outVal;
	}


}
