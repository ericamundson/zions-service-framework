package com.zions.testlink.services.test.handlers

import br.eti.kinoshita.testlinkjavaapi.model.TestCase
import org.springframework.stereotype.Component

@Component('TlTestCaseNameHandler')
class TestCaseNameHandler extends TlBaseAttributeHandler {
	static int SIZE = 255

	public String getFieldName() {
		
		return 'name'
	}

	public def formatValue(def value, def data) {
		TestCase tc = data.itemData
		String outVal = "${tc.id}: ${value}"
		if (outVal.length() > SIZE) {
			outVal = outVal.substring(0, SIZE-1)
		}
		return outVal;
	}

}
