package com.zions.bb.services.code

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.zions.bb.services.rest.BBGenericRestClient
import com.zions.clm.services.rest.ClmGenericRestClient

@Component
class BBCodeManagementService {
	@Autowired(required=false)
	private BBGenericRestClient bBGenericRestClient;

	public BBCodeManagementService() {
		
	}
	
	public def getProjectRepoData(String project) {
		
	}
}
