package com.zions.qm.services.test.handlers

import org.springframework.stereotype.Component

@Component
class BlankHandler extends QmBaseAttributeHandler {
	static int SIZE = 255

	public String getQmFieldName() {
		// TODO Auto-generated method stub
		return 'none'
	}

	public String formatValue(String value) {
		return '';
	}

}
