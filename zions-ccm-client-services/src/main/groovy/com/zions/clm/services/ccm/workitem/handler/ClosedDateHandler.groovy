package com.zions.clm.services.ccm.workitem.handler

import com.ibm.team.links.client.ILinkManager
import com.ibm.team.links.common.ILink
import com.ibm.team.links.common.ILinkCollection
import com.ibm.team.links.common.ILinkQueryPage
import com.ibm.team.links.common.IReference
import com.ibm.team.links.common.factory.IReferenceFactory
import com.ibm.team.repository.client.IItemManager
import com.ibm.team.repository.common.IAuditableHandle
import com.ibm.team.scm.common.IChangeSet
import com.ibm.team.workitem.common.model.IWorkItem
import com.zions.clm.services.ccm.client.RtcRepositoryClient
import com.zions.clm.services.ccm.workitem.WorkitemAttributeManager
import com.zions.common.services.work.handler.IFieldHandler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ClosedDateHandler implements IFieldHandler {
	@Autowired
	RtcRepositoryClient rtcRepositoryClient
	@Autowired
	WorkitemAttributeManager workitemAttributeManager
	
	def mappingData = null;

	public ImplementedDateHandler() {}


	public Object execute(Object data) {
		IWorkItem wi = data.workItem
		def fieldMap = data.fieldMap
		def wiCache = data.cacheWI
	    IItemManager itm = rtcRepositoryClient.getRepo().itemManager(); 
	    List history = itm.fetchAllStateHandles((IAuditableHandle) wi.getStateHandle(), rtcRepositoryClient.getMonitor());
		String state = determineState(wi)
		Date recordModifiedDate = wi.modified();
		def retVal = null
//		if ("${state}" == 'Implemented') {
//			def val = recordModifiedDate.format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
//			retVal = [op:'add', path:"/fields/${fieldMap.target}", value: val]
//		}
	    for(int i = history.size() -1; i >= 0; i--){
			if (retVal != null && "${state}" != 'Done') {
				break;
			}
			if ("${state}" == 'Done') {
				def val = recordModifiedDate.format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
				retVal = [op:'add', path:"/fields/${fieldMap.target}", value: val]
			}
	        IAuditableHandle audit = (IAuditableHandle) history.get(i);
			
	        IWorkItem workItemPrevious = (IWorkItem) rtcRepositoryClient.getRepo().itemManager().fetchCompleteState(audit,null);
	        recordModifiedDate = workItemPrevious.modified();
			state = determineState(workItemPrevious)
	    }
		if (wiCache != null) {
			def cVal = wiCache.fields["${fieldMap.target}"]
			def nVal = null
			if (retVal != null) {
				nVal = retVal.value
			}
			if ("${cVal}" == "${nVal}") {
				return null
			}
		}
		return retVal;
	}
	
	String determineState(IWorkItem wi) {
		return workitemAttributeManager.getStringRepresentation(wi, wi.getProjectArea(), 'internalState')
	}

}
