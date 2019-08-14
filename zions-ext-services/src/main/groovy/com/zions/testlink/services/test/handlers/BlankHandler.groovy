package com.zions.testlink.services.test.handlers

import org.springframework.stereotype.Component

/**
 * Handler used to set default values on a field.
 * 
 * @author z091182
 *
 */
@Component('TlBlankHandler')
class BlankHandler extends TlBaseAttributeHandler {
	static int SIZE = 255

	public String getFieldName() {
		return 'none'
	}

	public def formatValue(def value, def itemData) {
		return '';
	}

}
