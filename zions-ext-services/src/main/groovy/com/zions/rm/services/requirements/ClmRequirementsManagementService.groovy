package com.zions.rm.services.requirements

import com.zions.rm.services.rest.RmGenericRestClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * @author z091182
 * 
 * @startuml
 * 
 * annotation Component
 * annotation Autowired
 * 
 * class ClmRequirementsManagementService {
 * ... Ideas on methods to implement ...
 * + def queryForModules(String project, String query )
 * + def nextPage(String url)
 * }
 * note left: @Component
 * 
 * ClmRequirementsManagementService .. Component: Is as Spring component
 * ClmRequirementsManagementService .. Autowired: Has autowired dependencies
 * ClmRequirementsManagementService o--> RmGenericRestClient: @Autowire rmGenericRestClient
 * 
 * @enduml
 *
 */
@Component
class ClmRequirementsManagementService {
	
	@Autowired
	RmGenericRestClient rmGenericRestClient
	
	def queryForModules(String project, String query ) {
		
	}
	
	def nextPage(String url) {
		
	}

}
