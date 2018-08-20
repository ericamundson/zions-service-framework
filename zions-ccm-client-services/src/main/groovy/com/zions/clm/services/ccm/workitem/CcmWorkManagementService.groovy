package com.zions.clm.services.ccm.workitem

import org.springframework.beans.factory.annotation.Autowired
import com.ibm.team.repository.client.ITeamRepository
import com.ibm.team.workitem.client.IWorkItemClient
import com.ibm.team.workitem.common.model.IWorkItem
import com.zions.clm.services.ccm.client.RtcRepositoryClient

/**
 * Provides behavior to process RTC work items in a form to be used to translate to VSTS work items.
 * 
 * @author z091182
 *
 */
class CcmWorkManagementService {
	
	@Autowired
	RtcRepositoryClient rtcRepositoryClient
	@Autowired
	WorkitemAttributeManager workitemAttributeManager

	public CcmWorkManagementService() {
		
	}
	
	def getWIChanges(id, project, translateMapping) {
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		IWorkItemClient workItemClient = teamRepository.getClientLibrary(IWorkItemClient.class)
		IWorkItem workItem = workItemClient.findWorkItemById(id, IWorkItem.FULL_PROFILE, null);
		//workItem.
	}
}
