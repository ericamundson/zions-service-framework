package com.zions.vsts.services

															  
import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.rmq.mixins.MessageReceiverTrait
import com.zions.vsts.services.ws.client.AbstractWebSocketMicroService
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
	String[] types
	
	@Value('${tfs.cstatetrigger}')
	String cstateTrigger
	
	@Value('${tfs.project.includes}')
	String[] includeProjects
	
	// Handle HTTP 412 retry when work item revision has changed
	boolean retryFailed
	def attemptedProject
	def attemptedParentId
	def attemptedActivation
	Closure responseHandler = { resp ->
		if (resp.status == 412) {
			// Get fresh copy of parent work item
			def parentWI = workManagementService.getWorkItem(collection, attemptedProject, attemptedParentId)
			//unsure if this should be childState or Pstate?
			def pState = parentWI.fields.'System.State'
			String rev = "${parentWI.rev}"
			
			if (pState == 'New') {// Process if still inactive
			
				if (performParentActivation(this.attemptedProject, this.attemptedParentId, rev, this.attemptedActivation)) {
					return logResult('Work item successfully activated after 412 retry')
				}
				else {
					this.retryFailed = true
					log.error('Failed update after 412 retry')
					return 'Failed update after 412 retry'
				}
	
			}
			return
		}
	}

	public ParentActivationMicroService() {
			
	}
	
	 /**
	 * Perform parent activation on state changes for Task work items.
	 *
	 * @see com.zions.vsts.services.ws.client.AbstractWebSocketMicroService#processADOData(java.lang.Object)
	 */
	@Override
	public Object processADOData(Object adoData) {
		log.debug("Entering ParentActivationMicroService:: processADOData")
		
		/**		Uncomment code below to capture adoData payload for test*/
		/* String json = new JsonBuilder(adoData).toPrettyString()
		 println(json)*/
		
		
		def outData = adoData
		def wiResource = adoData.resource
		
		//**Check for qualifying projects how to setup in runtime settings?
		String project = "${wiResource.revision.fields.'System.TeamProject'}"
		if (includeProjects && !includeProjects.contains(project))
			return logResult('Project not included')
		
		
		String wiType = "${wiResource.revision.fields.'System.WorkItemType'}"
		
		String childState = "${wiResource.revision.fields.'System.State'}"
		
		//code to address null pointer exception
		if (!wiResource.fields || !wiResource.fields.'System.State') return logResult('No valid changes made')
	
		if (!types.contains(wiType))return logResult('not a valid work item type')
		
		if (!cstateTrigger.contains(childState)) return logResult('Not a target work item type')
			
		//this is child id
		String id = "${wiResource.revision.id}"
										   
		String parentId = "${wiResource.revision.fields.'System.Parent'}"
		
		//check to see if parent is assigned to child work item
		if (!parentId || parentId == 'null' || parentId == '') return logResult('parent not assigned')
		def parentWI = workManagementService.getWorkItem(collection, project, parentId)
		if (!parentWI) {
			log.error("Error retrieving work item $parentId")
			return 'Error Retrieving Parent'
		}
		if (!parentWI.fields || parentWI.fields == null || !parentWI.fields.'System.State') {
			log.error("Error retrieving work item $parentId")
			return 'parent does not exist'
		}
		/**	 For unit testing !! Uncomment code below to capture parent playload for test
		 * String json = new JsonBuilder(parentWI).toPrettyString()
		 * println(json)*/
			
		
		def pState = parentWI.fields.'System.State'
		String rev = "${parentWI.rev}"
		
		//If parent state is new perform activation steps
		if (pState == 'New') {
			log.debug("Updating parent of $wiType #$id")
		
			if (performParentActivation(project, rev, parentId, responseHandler)) {
				return logResult('Update Succeeded')
			}

			else if (this.retryFailed) {
				log.error('Error updating System.State')
				return 'Error updating System.State'
			}
		}
		
		else {
			
			return logResult('parent is already active')
			
		}
	}
	private def performParentActivation(String project, String rev, def parentId, def pState, Closure respHandler = null) {

		def data = []
		def t = [op: 'test', path: '/rev', value: rev.toInteger()]
		data.add(t)
		
		def e = [op: 'add', path: '/fields/System.State', value: 'Active']
		data.add(e)
		this.retryFailed = false
		this.attemptedProject = project
		this.attemptedParentId = parentId
		this.attemptedActivation = pState
		
		return workManagementService.updateWorkItem(collection, project, parentId, data, respHandler)
		
	}
	
	private def logResult(def msg) {
	
		log.debug(msg)
		return msg
	}
	
		
}
																						 
  