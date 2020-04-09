package com.zions.vsts.services

import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.ws.client.AbstractWebSocketMicroService
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
class SetOwnerMicroService extends AbstractWebSocketMicroService {

	@Autowired
	WorkManagementService workManagementService
	
	@Value('${tfs.collection:}')
	String collection	
	
	@Autowired
	public SetOwnerMicroService(@Value('${websocket.url:}') websocketUrl, 
		@Value('${websocket.user:#{null}}') websocketUser,
		@Value('${websocket.password:#{null}}') websocketPassword) {
		super(websocketUrl, websocketUser, websocketPassword)
		
	}

	/**
	 * Perform rollup calculations.
	 * 
	 * @see com.zions.vsts.services.ws.client.AbstractWebSocketMicroService#processADOData(java.lang.Object)
	 */
	@Override
	public Object processADOData(Object adoData) {
		log.info("Entering RollupMicroService:: processADOData")
		def outData = adoData
		def wiResource = adoData.resource
		String wiType = "${wiResource.revision.fields.'System.WorkItemType'}"
		String owner = "${wiResource.revision.fields.'System.AssignedTo'}"
		if (!wiType && wiType != 'Task') return null
		if (owner) return null
		if (!wiResource.fields) return null
		String parent = "${wiResource.revision._links.parent.href}"
		if ( parent ) {
			String parentID = ""
			String project = "${wiResource.revision.fields.'System.TeamProject'}"
			def parentWI = workManagementService.getWorkItem(collection, project, parentID)
		}
		return null;
	}

	@Override
	public String topic() {
		return 'workitem.updated';
	}

}

