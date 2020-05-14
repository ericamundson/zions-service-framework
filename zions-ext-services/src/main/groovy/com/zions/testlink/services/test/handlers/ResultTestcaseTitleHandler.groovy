package com.zions.testlink.services.test.handlers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import br.eti.kinoshita.testlinkjavaapi.model.TestCase
import com.zions.qm.services.test.ClmTestManagementService

@Component('TlResultTestcaseTitleHandler')
class ResultTestcaseTitleHandler extends TlBaseAttributeHandler {

	public ResultTestcaseTitleHandler() {
		cacheCheck = false
	}
	public String getFieldName() {
		
		return 'title'
	}

	public def formatValue(def value, def data) {
		def outVal = null
		TestCase testCase = data.testCase
		String title = "${testCase.name}"
		
		title = title.replaceAll(/[\u00a0]/, " ")
		
		return title.trim();
	}

}
