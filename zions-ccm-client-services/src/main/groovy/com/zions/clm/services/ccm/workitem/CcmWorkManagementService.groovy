package com.zions.clm.services.ccm.workitem

import org.springframework.beans.factory.annotation.Autowired

import com.zions.clm.services.ccm.client.RtcRepositoryClient

class CcmWorkManagementService {
	
	@Autowired
	RtcRepositoryClient rtcRepositoryClient

	public CcmWorkManagementService() {
		
	}
	
}
