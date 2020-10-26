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

	def calcDaysToClose(Date closedDate, Date createdDate) {
		// If wi is still open we set daysToClose to 0
		if (!closedDate) return 0
		
		def duration = groovy.time.TimeCategory.minus(closedDate,createdDate)
		
		String strClosedDate = closedDate.format('dd-MMM-yyyy')
		String strCreatedDate = createdDate.format('dd-MMM-yyyy')
		if (strClosedDate == strCreatedDate) 
			return 0.5 // Return half day for same day closure
		else
			return duration.days + new BigDecimal(duration.hours / 24).round()
	}

}

