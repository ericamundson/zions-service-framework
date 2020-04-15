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
		String status = "${wiResource.revision.fields.'System.State'}"
		if (!wiType && wiType != 'Task') return null
		if (owner != 'null') return null
		if (!status || status != 'Closed') return null
		String project = "${wiResource.revision.fields.'System.TeamProject'}"
		String id = "${wiResource.revision.id}"
		String parentId = "${wiResource.revision.fields.'System.Parent'}"
		if (parentId) setToParentOwner(project, id, parentId)
		return null;
	}

	private def setToParentOwner(def project, def id, def parentId) {
		// First get parent wi data
		def parentWI = workManagementService.getWorkItem(collection, project, parentId)
		def parentOwner = parentWI.fields.'System.AssignedTo'
		if (parentWI && parentOwner) {
			def data = []
			def t = [op: 'test', path: '/rev', value: parentWI.rev]
			data.add(t)
			def e = [op: 'add', path: '/fields/System.AssignedTo', value: parentOwner.uniqueName]
			data.add(e)
			workManagementService.updateWorkItem(collection, project, id, data)
		}

	}

	@Override
	public String topic() {
		return 'workitem.updated';
	}

}

