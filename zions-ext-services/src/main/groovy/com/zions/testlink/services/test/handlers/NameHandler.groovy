package com.zions.testlink.services.test.handlers

import java.util.regex.Matcher
import java.util.regex.Pattern
import org.springframework.stereotype.Component

@Component('TlNameHandler')
class NameHandler extends TlBaseAttributeHandler {
	static int SIZE = 255

	public String getFieldName() {

		return 'name'
	}

	public def formatValue(def value, def data) {
		String outVal = "${value}"
		if (value.length() > SIZE) {
			outVal = value.substring(0, SIZE-1)
		}
		outVal = outVal.replaceAll(/[\u2018\u2019​]/, "'")
		return outVal;
	}

}
