package com.zions.qm.services.test.handlers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.qm.services.test.ClmTestManagementService

@Component
class ResultTestcaseTitleHandler extends QmBaseAttributeHandler {
	@Autowired
	ClmTestManagementService clmTestManagementService

	public String getQmFieldName() {
		// TODO Auto-generated method stub
		return 'title'
	}

	public def formatValue(def value, def data) {
		def outVal = null
		def itemData = data.itemData
		String title = "${itemData.title.text()}"
		return title;
	}

}
