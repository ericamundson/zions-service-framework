package com.zions.testlink.services.test.handlers

import br.eti.kinoshita.testlinkjavaapi.model.TestCase
import com.zions.common.services.cache.ICacheManagementService
import com.zions.qm.services.test.ClmTestItemManagementService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component('TlResultTestCaseReferenceHandler')
class ResultTestCaseReferenceHandler extends TlBaseAttributeHandler {
	static int SIZE = 255
	
	@Autowired
	ICacheManagementService cacheManagementService

	public String getFieldName() {
		
		return 'title'
	}

	public def formatValue(def value, def data) {
		TestCase testCase = data.testCase
		String tId = "${testCase.id}"
		def adoTestCase = cacheManagementService.getFromCache(tId, ICacheManagementService.WI_DATA)
		if (adoTestCase == null) return null
		//def outVal = [id: adoTestCase.id, name: "${adoTestCase.fields.'System.Title'}", url: "${adoTestCase._links.self.href}"]
		def outVal = Integer.parseInt("${adoTestCase.id}")
		return outVal;
	}

}
