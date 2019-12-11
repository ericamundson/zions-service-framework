package com.zions.qm.services.test.handlers

import org.springframework.stereotype.Component

@Component('QmResultCompletedDateHandler')
class ResultCompletedDateHandler extends QmBaseAttributeHandler {
	static int SIZE = 255

	public String getQmFieldName() {
		
		return 'endtime'
	}

	public def formatValue(def value, def data) {
		def itemData = data.itemData
		String outVal = "${value}"
		
		if (value.length() == 0) {
			return null
		}
		Date modDate = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", outVal)
		
		def sVal = itemData.starttime.text()
		if (sVal.length() == 0) return outVal
		String sStr = "${sVal}"
		Date sDate = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sStr)
		if (sDate.time > modDate.time) return sStr
		return outVal;
	}

}
