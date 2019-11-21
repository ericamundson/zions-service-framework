package com.zions.qm.services.test.handlers

import com.zions.common.services.cache.ICacheManagementService
import com.zions.qm.services.test.ClmTestItemManagementService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component('QmResultTestCaseReferenceHandler')
class ResultTestCaseReferenceHandler extends QmBaseAttributeHandler {
	static int SIZE = 255
	
	@Autowired
	ICacheManagementService cacheManagementService

	public String getQmFieldName() {
		
		return 'title'
	}

	public def formatValue(def value, def data) {
		def testCase = data.testCase
		String tId = "${testCase.webId.text()}-Test Case"
		def adoTestCase = cacheManagementService.getFromCache(tId, ICacheManagementService.WI_DATA)
		if (adoTestCase == null) return null
		//def outVal = [id: adoTestCase.id, name: "${adoTestCase.fields.'System.Title'}", url: "${adoTestCase._links.self.href}"]
		def outVal = Integer.parseInt("${adoTestCase.id}")
		return outVal;
	}

}
