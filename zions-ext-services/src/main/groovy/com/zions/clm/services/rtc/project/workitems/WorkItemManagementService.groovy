package com.zions.clm.services.rtc.project.workitems;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zions.clm.services.rest.ClmGenericRestClient;

@Component
public class WorkItemManagementService {

	@Autowired
	ClmGenericRestClient clmGenericRestClient
	
	public WorkItemManagementService() {
		
	}
}
