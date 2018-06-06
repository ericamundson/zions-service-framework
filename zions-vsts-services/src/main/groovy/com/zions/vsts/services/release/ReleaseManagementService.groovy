package com.zions.vsts.services.release;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component
import com.zions.vsts.services.tfs.rest.GenericRestClient;

@Component
public class ReleaseManagementService {
	@Autowired
	private GenericRestClient genericRestClient

	public ReleaseManagementService() {
		
	}
	
	public def ensureReleases(def collection, def project) {
		
	}
}
