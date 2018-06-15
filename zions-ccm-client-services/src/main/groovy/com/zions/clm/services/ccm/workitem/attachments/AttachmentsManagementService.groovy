package com.zions.clm.services.ccm.workitem.attachments;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component;
import com.ibm.team.repository.client.ITeamRepository
import com.zions.clm.services.ccm.client.RtcRepositoryClient;

@Component
public class AttachmentsManagementService {
	
	@Autowired
	RtcRepositoryClient rtcRepositoryClient
	
	String cacheLocation
	
	@Autowired
	public AttachmentsManagementService(@Value('${cache.location}') String cacheLocation) {
		this.cacheLocation = cacheLocation
	}
	
	public def cacheWorkItemAttachments(int id) {
		ITeamRepository repo = rtcRepositoryClient.getRepo()
	}

}
