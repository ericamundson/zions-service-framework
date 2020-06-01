package com.zions.vsts.services.setowner

import com.zions.vsts.services.rmq.mixins.MessageReceiverTrait
import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.ws.client.AbstractWebSocketMicroService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import groovy.json.JsonBuilder

/**
 * Assigns unassigned tasks to the parent's owner when the task is closed.
 * 
 * @author z097331
 *
 */
@Component
@Slf4j
class SetOwnerMicroService implements MessageReceiverTrait {

	@Autowired
	WorkManagementService workManagementService

	@Value('${tfs.collection:}')
	String collection	

	@Value('${tfs.types}')
	String wiTypes

	@Autowired
	public SetOwnerMicroService() {		
	}

	/**
	 * Perform assignment operation
	 * 
	 * @see com.zions.vsts.services.ws.client.AbstractWebSocketMicroService#processADOData(java.lang.Object)
	 */
	@Override
	public Object processADOData(Object adoData) {
		log.info("Entering SetOwnerMicroService:: processADOData")
//		Uncomment code below to capture adoData payload for test
//		String json = new JsonBuilder(adoData).toPrettyString()
//		println(json)
		def types = wiTypes.split(',')
		def outData = adoData
		def wiResource = adoData.resource
		String wiType = "${wiResource.revision.fields.'System.WorkItemType'}"
		String owner = "${wiResource.revision.fields.'System.AssignedTo'}"
		String status = "${wiResource.revision.fields.'System.State'}"
		if (!types.contains(wiType)) return logResult('Not a target work item type')
		if (owner != 'null') return logResult('Work item already assigned')
		if (status != 'Closed') return logResult('Work item not closed')
		String project = "${wiResource.revision.fields.'System.TeamProject'}"
		String id = "${wiResource.revision.id}"
		String rev = "${wiResource.revision.rev}"
		String parentId = "${wiResource.revision.fields.'System.Parent'}"
		if (parentId != 'null') {
			// First get parent wi data
			def parentWI = workManagementService.getWorkItem(collection, project, parentId)
//			Uncomment code below to capture parent playload for test
//			String json = new JsonBuilder(parentWI).toPrettyString()
//			println(json)
			def parentOwner = parentWI.fields.'System.AssignedTo'
			if (parentOwner == 'null' || parentOwner == null) return logResult('Parent is unassigned')
			
			log.info("Updating owner of $wiType #$id")
			try {
				setToParentOwner(project, id, rev, parentOwner)
				return logResult('Work item successfully assigned')
			}
			catch (e){
				log.error("Error updating System.AssigedTo: ${e.message}")
				return 'Error assigning work item'
			}
		}
		else {
			return logResult('No parent')
		}
	}

	private def setToParentOwner(def project, def id, String rev, def parentOwner) {
		def data = []
		def t = [op: 'test', path: '/rev', value: rev.toInteger()]
		data.add(t)
		def e = [op: 'add', path: '/fields/System.AssignedTo', value: parentOwner.uniqueName]
		data.add(e)
		workManagementService.updateWorkItem(collection, project, id, data)
	}
	private def logResult(def msg) {
		log.info("Result: $msg")
		return msg
	}

}

