package com.zions.vsts.services.work.calculations

import com.zions.vsts.services.work.calculations.handlers.BaseCalcHandler
import com.zions.vsts.services.work.calculations.handlers.ColorCalcHandler
import com.zions.common.services.work.handler.IFieldHandler
import com.zions.vsts.services.work.WorkManagementService
import groovy.transform.Canonical
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Handle work field calculations.
 * 
 * @author z091182
 *
 */
@Component
@Slf4j
class CalculationManagementService {
	
	@Autowired(required=false)
	Map<String, BaseCalcHandler> fieldMap;

	public CalculationManagementService() {
		
	}
	

	def execute(def data, String handler) {
		if (this.fieldMap["${handler}"] != null) {
			def calcValue = this.fieldMap["${handler}"].execute(data)
			return calcValue
		}
		else {
			throw new Exception("Handler not found: ${handler}")
		}

	}


}

