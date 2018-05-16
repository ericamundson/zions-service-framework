package com.zions.vsts.services.admin.user.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zions.vsts.services.tfs.rest.GenericRestClient;

@Component
public class MemberManagementService {
	@Autowired(required=true)
	private GenericRestClient genericRestClient;

	public MemberManagementService() {
		
	}
	
	public def addMember(String id, String role, String[] projects) {
		return null;
	
	}
}
