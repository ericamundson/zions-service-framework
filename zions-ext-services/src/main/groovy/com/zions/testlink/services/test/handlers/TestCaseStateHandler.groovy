package com.zions.testlink.services.test.handlers

import br.eti.kinoshita.testlinkjavaapi.model.TestCase
import br.eti.kinoshita.testlinkjavaapi.model.TestPlan
import com.zions.qm.services.test.ClmTestManagementService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component('TlTestCaseStateHandler')
class TestCaseStateHandler extends TlBaseAttributeHandler {
	
	public String getFieldName() {
		
		return 'state'
	}

	public def formatValue(def value, def data) {
		TestCase itemData = data.itemData
		return 'Design';
	}

}
