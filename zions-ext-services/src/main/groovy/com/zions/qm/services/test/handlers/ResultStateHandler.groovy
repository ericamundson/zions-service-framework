package com.zions.qm.services.test.handlers

import org.springframework.stereotype.Component

@Component
class ResultStateHandler extends QmBaseAttributeHandler {
	static int SIZE = 255

	public String getQmFieldName() {
		// TODO Auto-generated method stub
		return 'state'
	}

	public def formatValue(def value, def data) {
		def itemData = data.itemData
		String outVal = "${itemData.state.text()}"
		return outVal;
	}

}
