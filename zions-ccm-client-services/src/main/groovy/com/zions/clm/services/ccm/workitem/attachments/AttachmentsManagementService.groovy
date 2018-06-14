package com.zions.clm.services.ccm.workitem.attachments;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zions.clm.services.ccm.client.RtcRepositoryClient;

@Component
public class AttachmentsManagementService {
	
	@Autowired
	RtcRepositoryClient rtcRepositoryClient
	
	public AttachmentsManagementService() {
		
	}
	
	public def getWorkItemAttachments(String projectArea, int id) {
		
	}

}
