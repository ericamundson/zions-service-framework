package com.zions.qm.services.test.handlers

import com.zions.common.services.cache.ICacheManagementService
import com.zions.qm.services.test.ClmTestItemManagementService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component('QmResultIdHandler')
class ResultIdHandler extends QmBaseAttributeHandler {
	static int SIZE = 255
	
	@Autowired
	ICacheManagementService cacheManagementService
	
	public ResultIdHandler() 
	{
		this.cacheCheck = false
	}

	public String getQmFieldName() {
		// TODO Auto-generated method stub
		return 'title'
	}

	public def formatValue(def value, def data) {
		def testCase = data.testCase
		def cacheData = data.cacheWI
		if (cacheData == null) return null
		//def outVal = [id: adoTestCase.id, name: "${adoTestCase.fields.'System.Title'}", url: "${adoTestCase._links.self.href}"]
		def outVal = Integer.parseInt("${cacheData.id}")
		return outVal;
	}

}
