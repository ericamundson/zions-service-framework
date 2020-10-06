package com.zions.vsts.services.work.calculations.handlers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import com.zions.vsts.services.asset.SharedAssetService
import com.zions.vsts.services.work.calculations.CalculationManagementService
import groovy.util.logging.Slf4j

import com.zions.common.services.work.handler.IFieldHandler
import org.springframework.stereotype.Component

/**
 * Base class for producing work item field entries.
 * 
 * @author z091182
 * 
 * @startuml
 * 
 * abstract class RmBaseAttributeHandler {
 * + def execute(def data)
 * + <<abstract>> String getFieldName()
 * + <<abstract>> def formatValue(def val, def itemData)
 * }
 * 
 * IFieldHandler <|.. RmBaseAttributeHandler
 * 
 * @enduml
 * 
 *
 */
@Component
@Slf4j
public class DaysToCloseHandler extends BaseCalcHandler {
	@Autowired
	CalculationManagementService calcManagementService
	
	public String execute(String targetField, def fields) {
		def closedDate = fields['Closed Date']
		// throw error if no Closed Date column
		if (closedDate == null) // missing column
			throw new Exception("Closed Date is required by handler: ${this.getClass().getName()}")
		else if (closedDate == '') // no value (which is ok)
			closedDate = null  // Work Items that are still open will not have a closed date.  must be set to null
		def createdDate = fields['Created Date']
		// throw error if no Created Date
		if (!createdDate)
			throw new Exception("Created Date is required by handler: ${this.getClass().getName()}")
		//Get work item
		return calcManagementService.calcDaysToClose(closedDate, createdDate)
	}
}
