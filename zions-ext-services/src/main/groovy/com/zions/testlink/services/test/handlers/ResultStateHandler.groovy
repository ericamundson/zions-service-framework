package com.zions.testlink.services.test.handlers

import br.eti.kinoshita.testlinkjavaapi.model.Execution
import org.springframework.stereotype.Component

@Component('TlResultStateHandler')
class ResultStateHandler extends TlBaseAttributeHandler {
	static int SIZE = 255

	public String getFieldName() {
		
		return 'state'
	}

	public def formatValue(def value, def data) {
		Execution itemData = data.itemData
		String outVal = "${itemData.status}"
		return outVal.trim();
	}

}
