package com.zions.qm.services.test.handlers

import org.springframework.stereotype.Component

@Component
class SummaryHandler extends QmBaseAttributeHandler {

	public String getQmFieldName() {
		// TODO Auto-generated method stub
		return 'summary'
	}

	public def formatValue(def value, def data) {
		String outVal = "${value}"
		if (value == null || value.length() == 0) return null
		return outVal;
	}

}
