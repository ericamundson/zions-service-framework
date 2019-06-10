package com.zions.qm.services.test.handlers

import org.springframework.stereotype.Component

@Component('QmBlankHandler')
class BlankHandler extends QmBaseAttributeHandler {
	static int SIZE = 255

	public String getQmFieldName() {
		return 'none'
	}

	public def formatValue(def value, def itemData) {
		return '';
	}

}
