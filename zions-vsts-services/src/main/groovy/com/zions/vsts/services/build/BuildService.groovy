package com.zions.vsts.services.build;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zions.vsts.services.tfs.rest.GenericRestClient;

@Component
public class BuildService {
	
	@Autowired
	private GenericRestClient genericRestClient
	
	public BuildService() {
		
	}
	
	public def provideTag(def buildData) {
		
	}

}
