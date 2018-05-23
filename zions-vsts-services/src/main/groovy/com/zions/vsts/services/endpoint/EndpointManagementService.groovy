package com.zions.vsts.services.endpoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zions.vsts.services.tfs.rest.GenericRestClient;

@Component
public class EndpointManagementService {
	@Autowired
	private GenericRestClient genericRestClient
	
	public EndpointManagementService() {
		
	}
	
	public def createServiceEndpoint(String collection, String projectId) {
		
	}

}
