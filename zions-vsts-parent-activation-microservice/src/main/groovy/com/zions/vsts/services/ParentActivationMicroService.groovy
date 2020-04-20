package com.zions.vsts.services

import com.zions.vsts.services.work.WorkManagementService

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
	WorkManagementService workManagementService
	
	@Value('${tfs.collection:}')
	String collection
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
		log.info("Entering ParentActivationMicroService:: processADOData")
		def outData = adoData
		def wiResource = adoData.resource
		String wiType = "${wiResource.revision.fields.'System.WorkItemType'}"
		
		String childState = "${wiResource.revision.fields.'System.State'}"
		if (!wiType && wiType != 'Task') return null
		if (!wiResource.fields) return null
		String project = "${wiResource.revision.fields.'System.TeamProject'}"
		String id = "${wiResource.revision.id}"
		String rev = "${wiResource.revision.rev}"
		String parentId = "${wiResource.revision.fields.'System.Parent'}"
		if (childState) performParentActivation(childState, project, id, rev, parentId)
		return null;
		
	}
	//getting parent data
	private def performParentActivation(def childState, def project, def id, String rev, def parentId) {
		
		// First get parent wi data
		def parentWI = workManagementService.getWorkItem(collection, project, parentId)
		def pState = parentWI.fields.'System.State'

		//if (parentWI) {
		if (((parentWI) && childState == 'Closed' || childState == 'Active') && pState == 'New') {
		//if ((childState == 'Closed' || childState == 'Active') && pState == 'New') {
			
			def data = []
			def t = [op: 'test', path: '/rev', value: rev.toInteger()]
			data.add(t)
			
			def e = [op: 'add', path: '/fields/System.State', value: 'Active']
			//def e = [op: 'add', path: '/fields/System.State', value: pState.'Active']
			
			data.add(e)
			workManagementService.updateWorkItem(collection, project, id, data)
		}
	}
	

	@Override
	public String topic() {
		return 'workitem.updated';
	}

}