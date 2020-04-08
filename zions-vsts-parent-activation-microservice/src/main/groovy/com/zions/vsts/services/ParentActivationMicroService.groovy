package com.zions.vsts.services

import com.zions.vsts.services.work.calculations.ParentActivationManagementService
import com.zions.vsts.services.ws.client.AbstractWebSocketMicroService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Will activate parent work item when Task is activated.
 *
 * @author z070187
 *
 */
@Component
@Slf4j
class ParentActivationMicroService extends AbstractWebSocketMicroService {

	@Autowired
	ParentActivationManagementService parentActivationManagementService
	
//	@Autowired
//	public RollupMicroService(@Value('${websocket.url:}') websocketUrl) {
//		super(websocketUrl)
//
//	}
	
	@Autowired
	public ParentActivationMicroService(@Value('${websocket.url:}') websocketUrl,
		@Value('${websocket.user:#{null}}') websocketUser,
		@Value('${websocket.password:#{null}}') websocketPassword) {
		super(websocketUrl, websocketUser, websocketPassword)
		
	}

	/**
	 * Perform parent activation on state changes for Task work items.
	 *
	 * @see com.zions.vsts.services.ws.client.AbstractWebSocketMicroService#processADOData(java.lang.Object)
	 */
	@Override
	public Object processADOData(Object adoData) {
		log.info("ParentActivationMicroService:: processADOData")
		def outData = adoData
		def wiResource = adoData.resource
		String wiType = "${wiResource.revision.fields.'System.WorkItemType'}"
		//originalCode// if (!wiType && wiType != 'Task') return null
		if (!wiType || (wiType && wiType != 'Task')) return null
		if (!wiResource.fields) return null
		
		
		//If modified fields are found Declare new variable and pull modified System State on the Task
		//String isModifiedState = wiResource.fields.'System.State'
		String childState = wiResource.fields.'System.State'
		
		/*String isModifiedRemaining = wiResource.fields.'Microsoft.VSTS.Scheduling.RemainingWork'
		String isModifiedCompleted = wiResource.fields.'Microsoft.VSTS.Scheduling.CompletedWork'*/
		
		 //check for any tasks that have changed state to "Active" or "Closed"
		//old value if (isModifiedState == 'New' || 'Closed') {
		if (childState == 'New' || childState == 'Closed') {
		
			String id = "${wiResource.revision.id}"
			String project = "${wiResource.revision.fields.'System.TeamProject'}"
			try {
				//instantiate work management service
				parentActivationManagementService.performParentActivation(id, childState, project)
			} catch (e) {
				log.error("Failed parent activation :  ", e)
			}
		}
		return null;
	}

	@Override
	public String topic() {
		return 'workitem.updated';
	}

}