package com.zions.vsts.services.code

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.endpoint.EndpointManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient

@Component
class CodeManagementService {
	@Autowired
	private GenericRestClient genericRestClient
	
	@Autowired 
	private ProjectManagementService projectManagementService
	
	@Autowired
	private EndpointManagementService endpointManagementService

	public CodeManagementService() {
		
	}
	
	public def createRepo(String collection, String project, String repoName, String importUrl, String importUser, String importPassword) {
		
	}
}
