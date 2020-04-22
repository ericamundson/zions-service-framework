package com.zions.vsts.services

import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.ws.client.AbstractWebSocketMicroService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Assigns unassigned tasks to the parent's owner when the task is closed.
 * 
 * @author z097331
 *
 */
@Component
@Slf4j
class SetOwnerMicroService extends AbstractWebSocketMicroService {

	@Autowired
	WorkManagementService workManagementService
	
	@Value('${tfs.collection:}')
	String collection	

	@Value('${tfs.types:}')
	String wiTypes

	@Autowired
	public SetOwnerMicroService(@Value('${websocket.url:}') websocketUrl, 
		@Value('${websocket.user:#{null}}') websocketUser,
		@Value('${websocket.password:#{null}}') websocketPassword) {
		super(websocketUrl, websocketUser, websocketPassword)
		
	}

	/**
	 * Perform assignment operation
	 * 
	 * @see com.zions.vsts.services.ws.client.AbstractWebSocketMicroService#processADOData(java.lang.Object)
	 */
	@Override
	public Object processADOData(Object adoData) {
		log.info("Entering SetOwnerMicroService:: processADOData")
		def types = wiTypes.split(',')
		def outData = adoData
		def wiResource = adoData.resource
		String wiType = "${wiResource.revision.fields.'System.WorkItemType'}"
		String owner = "${wiResource.revision.fields.'System.AssignedTo'}"
		String status = "${wiResource.revision.fields.'System.State'}"
		if (!wiType && !types.contains(wiType)) return null
		if (owner != 'null') return null
		if (!status || status != 'Closed') return null
		String project = "${wiResource.revision.fields.'System.TeamProject'}"
		String id = "${wiResource.revision.id}"
		String rev = "${wiResource.revision.rev}"
		String parentId = "${wiResource.revision.fields.'System.Parent'}"
		if (parentId) {
			log.info("Updating owner of $wiType #$id")
			try {
				setToParentOwner(project, id, rev, parentId)
				log.info("Updated succeeded")
			}
			catch (e){
				log.info("Error updating System.AssigedTo: ${e.message}")
			}
		}
		return null;
	}

	private def setToParentOwner(def project, def id, String rev, def parentId) {
		// First get parent wi data
		def parentWI = workManagementService.getWorkItem(collection, project, parentId)
		def parentOwner = parentWI.fields.'System.AssignedTo'
		if (parentWI && parentOwner) {
			def data = []
			def t = [op: 'test', path: '/rev', value: rev.toInteger()]
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

