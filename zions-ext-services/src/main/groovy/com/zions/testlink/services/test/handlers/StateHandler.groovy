package com.zions.testlink.services.test.handlers

import br.eti.kinoshita.testlinkjavaapi.model.TestPlan
import com.zions.qm.services.test.ClmTestManagementService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component('TlStateHandler')
class StateHandler extends TlBaseAttributeHandler {
	
	public String getFieldName() {
		
		return 'state'
	}

	public def formatValue(def value, def data) {
		TestPlan itemData = data.itemData
		if (itemData.isActive()) return 'Active'
		return 'Inactive';
	}

}
