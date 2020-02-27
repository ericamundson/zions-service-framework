package com.zions.testlink.services.test.handlers

import org.springframework.stereotype.Component

@Component('TlIdHandler')
class IdHandler extends TlBaseAttributeHandler {

	public String getFieldName() {
		return 'id'
	}

	public def formatValue(def value, def data) {
		def itemData = data.itemData
		return "TL-${itemData.id}";
	}


}
