package com.zions.qm.services.test.handlers

import org.springframework.stereotype.Component

@Component('QmResultStateHandler')
class ResultStateHandler extends QmBaseAttributeHandler {
	static int SIZE = 255

	public String getQmFieldName() {
		
		return 'state'
	}

	public def formatValue(def value, def data) {
		def itemData = data.itemData
		String outVal = "${itemData.state.text()}"
		return outVal.trim();
	}

}
