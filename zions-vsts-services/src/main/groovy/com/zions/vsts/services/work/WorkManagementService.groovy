package com.zions.vsts.services.work

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient

@Component
class WorkManagementService {
	
	@Autowired(required=true)
	private GenericRestClient genericRestClient;
	
	@Autowired(required=true)
	private ProjectManagementService projectManagementService;

	public WorkManagementService() {
		
	}
	
	def updateWorkitemTypeDefinition(def collection, def project, def witData) {
		
	}
	
	

}
