package com.zions.rm.services.requirements

import com.zions.common.services.work.handler.IFieldHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Handles all of the behavior to pull RM data for setting up output data to be sent to ADO to create/update work items.
 * 
 * Design<p/>
 * <img src="ClmRequirementsItemManagementService.png"/>
 * 
 * @author z091182
 * 
 * 
 * @startuml
 * 
 * annotation Component
 * annotation Autowired
 * 
 * class Map<String, IFieldHandler> {
 * }
 * 
 * class ClmRequirementsItemManagementService {
 * .... Idea on entry point method to implement ...
 * +def generateItemData(def rmItemData, def map, String project, def memberMap, def parent = null)
 * }
 * note left: @Component
 * 
 * ClmRequirementsItemManagementService .. Component: Spring component
 * ClmRequirementsItemManagementService .. Autowired: Spring injects instance
 * ClmRequirementsItemManagementService o--> Map: @Autowired fieldMap
 * 
 * 
 * @enduml
 *
 */
@Component
class ClmRequirementsItemManagementService {
	@Autowired
	Map<String, IFieldHandler> fieldMap
	
	def generateItemData(def rmItemData, def map, String project, def memberMap, def parent = null) {
	}

}
