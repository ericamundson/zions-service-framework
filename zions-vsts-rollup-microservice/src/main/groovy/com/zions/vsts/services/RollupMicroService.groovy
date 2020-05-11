package com.zions.vsts.services

import com.zions.vsts.services.rmq.mixins.MessageReceiverTrait
import com.zions.vsts.services.work.calculations.RollupManagementService
//import com.zions.vsts.services.ws.client.WebSocketMicroServiceTrait

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Handles rollup calculations of work data from Task to Feature work item types.
 * 
 * @author z091182
 *
 */
@Component
@Slf4j
class RollupMicroService implements MessageReceiverTrait {

	@Autowired
	RollupManagementService rollupManagementService
	
//	@Autowired
//	public RollupMicroService(@Value('${websocket.url:}') websocketUrl) {
//		super(websocketUrl)
//		
//	}
	
//	@Autowired
//	public RollupMicroService(@Value('${websocket.url:}') websocketUrl, 
//		@Value('${websocket.user:#{null}}') websocketUser,
//		@Value('${websocket.password:#{null}}') websocketPassword) {
//		init(websocketUrl, websocketUser, websocketPassword)
//		
//	}
	public RollupMicroService() {
		
	}

	/**
	 * Perform rollup calculations.
	 * 
	 * @see com.zions.vsts.services.ws.client.AbstractWebSocketMicroService#processADOData(java.lang.Object)
	 */
	public def processADOData(def adoData) {
		log.info("Entering RollupMicroService:: processADOData")
		def outData = adoData
		def wiResource = adoData.resource
		String wiType = "${wiResource.revision.fields.'System.WorkItemType'}"
		if (!wiType && wiType != 'Task') return null
		if (!wiResource.fields) return null
		String isModifiedEstimate = wiResource.fields.'Microsoft.VSTS.Scheduling.OriginalEstimate'
		String isModifiedRemaining = wiResource.fields.'Microsoft.VSTS.Scheduling.RemainingWork'
		String isModifiedCompleted = wiResource.fields.'Microsoft.VSTS.Scheduling.CompletedWork'
		if (isModifiedEstimate || isModifiedRemaining || isModifiedCompleted) {
			String id = "${wiResource.revision.id}"
			String project = "${wiResource.revision.fields.'System.TeamProject'}"
			try {
				rollupManagementService.performParentRollup(id, project)
			} catch (e) {
				log.error("Failed rollup:  ", e)
			}
		}
		return null;
	}

	public String topic() {
		return 'workitem.updated';
	}

}

