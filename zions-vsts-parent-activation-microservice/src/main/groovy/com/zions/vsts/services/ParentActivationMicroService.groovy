package com.zions.vsts.services

import com.zions.vsts.services.work.WorkManagementService

import com.zions.vsts.services.rmq.mixins.MessageReceiverTrait
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import groovy.json.JsonBuilder

/**
 * Will activate parent work item when child is activated.
 *
 * @author z070187
 *
 */
@Component
@Slf4j
class ParentActivationMicroService implements MessageReceiverTrait {

	@Autowired
	WorkManagementService workManagementService
	
	@Value('${tfs.collection:}')
	String collection
	
	//@Value('${tfs.types:}')
	@Value('${tfs.types}')
	String wiTypes


	public ParentActivationMicroService() 
	{
			
	}
	
	 /**
	 * Perform parent activation on state changes for Task work items.
	 *
	 * @see com.zions.vsts.services.ws.client.AbstractWebSocketMicroService#processADOData(java.lang.Object)
	 */
	@Override
	public Object processADOData(Object adoData) {
		log.info("Entering ParentActivationMicroService:: processADOData")
		
		/**		Uncomment code below to capture adoData payload for test
		 * String json = new JsonBuilder(adoData).toPrettyString()
		 * println(json) */
		
		
		def outData = adoData
		def wiResource = adoData.resource
		String wiType = "${wiResource.revision.fields.'System.WorkItemType'}"
		String childState = "${wiResource.revision.fields.'System.State'}"
		if (!wiType && !types.contains(wiType)) 
		{
			return logResult('not a valid work item type')
		
		}
		
		if (!(childState == 'Active' || childState == 'Closed'))
		{
			return logResult('not a valid child state')
		}
			
		
		String project = "${wiResource.revision.fields.'System.TeamProject'}"
		//this is child id
		String id = "${wiResource.revision.id}"
		String parentId = "${wiResource.revision.fields.'System.Parent'}"
	    
		//check to see if parent is assigned to child work item
		if (!parentId || parentId == 'null' || parentId == '') {
			
			return logResult('parent not assigned')
			
			}
		
		def parentWI = workManagementService.getWorkItem(collection, project, parentId)
		
		/**			For unit testing !! Uncomment code below to capture parent playload for test
		 * String json = new JsonBuilder(parentWI).toPrettyString()
		 * println(json)*/
		
		
		def pState = parentWI.fields.'System.State'
		String rev = "${parentWI.rev}"
		
		//If parent state is new perform activation steps
		if (pState == 'New') {
			log.info("Updating parent of $wiType #$id")
			try {
				performParentActivation(project, rev, parentId)
				
				return logResult('Update Succeeded')
				
			}
			catch (e){
				log.error("Error updating parent System.State: ${e.message}")
				return 'Failed update'
				
				
			}
		}
		else {
			
			return logResult('parent is already active')
			
	}
}			
	private def performParentActivation(String project, String rev, def parentId) {

		def data = []
		def t = [op: 'test', path: '/rev', value: rev.toInteger()]
		data.add(t)
		
		def e = [op: 'add', path: '/fields/System.State', value: 'Active']
		data.add(e)
		workManagementService.updateWorkItem(collection, project, parentId, data)
		
	}
	
	private def logResult(def msg)
	{
		log.info(msg)
		return msg
	}
	
		
}