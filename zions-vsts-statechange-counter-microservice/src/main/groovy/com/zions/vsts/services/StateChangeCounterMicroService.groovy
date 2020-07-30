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
 * Will increment a running bug reopen counter
  *
 * @author z070187
 *
 */

@Component
@Slf4j
class StateChangeCounterMicroService implements MessageReceiverTrait {

	@Autowired
	WorkManagementService workManagementService
	
	@Value('${tfs.collection:}')
	String collection

	//@Value('${tfs.types:}')
	@Value('${tfs.types}')
	String[] types

	@Value('${tfs.sourcestate}')
	String sourceState
	
	@Value('${tfs.deststate}')
	String destState
	
	@Value('${tfs.project.includes}')
	String[] includeProjects
	
	// Handle HTTP 412 retry when work item revision has changed
	boolean retryFailed
	def attemptedProject
	def attemptedId
	
	Closure responseHandler = { resp ->
		
		if (resp.status == 412) {
			
			// Get fresh copy of parent work item
			def bugWI = workManagementService.getWorkItem(collection, attemptedProject, attemptedId)
			
			//ado event structure different from work item structure - bugWI.fields represents work item structure
			def statechangeCount = bugWI.fields.'Custom.ReOpenCounter'
			String rev = "${bugWI.rev}"
			
			if (!statechangeCount || statechangeCount == 'null' || statechangeCount == '')
					statechangeCount = 0;
	
			//if (performIncrementCounter(this.attemptedProject, this.attemptedId, rev, statechangeCount)) {
			if (performIncrementCounter(this.attemptedProject, rev, this.attemptedId, statechangeCount)) {
				return logResult('Work item successfully counted after 412 retry')
			}
			else {
				this.retryFailed = true
				log.error('Failed update after 412 retry')
				return 'Failed update after 412 retry'
			}
			
		}
	}

	
	public StateChangeCounterMicroService(){
			
	}
	
	 /**
	 * Perform parent activation on state changes for Task work items.
	 *
	 * @see com.zions.vsts.services.ws.client.AbstractWebSocketMicroService#processADOData(java.lang.Object)
	 */
	@Override
	public Object processADOData(Object adoData) {
		log.debug("Entering StateChangeCounterMicroService:: processADOData")
		
		/**		Uncomment code below to capture adoData payload for test*/
		/* String json = new JsonBuilder(adoData).toPrettyString()
		 println(json)*/
		
		def outData = adoData
		def wiResource = adoData.resource
		
		//**Check for qualifying projects
		String project = "${wiResource.revision.fields.'System.TeamProject'}"
		if (includeProjects && !includeProjects.contains(project))
			return logResult('Project not included')
		
		//detect the work item type involved in the edit
		String wiType = "${wiResource.revision.fields.'System.WorkItemType'}"
		
		//If Bug execute counter code
		if (!types.contains(wiType))return logResult('not a valid work item type')
		
		//define a variable to get just fields System.State
		//def stateField = wiResource.fields.'System.State'
		
		//if (!stateField || stateField == 'null' || stateField == '') return logResult('not a state change')
	
		//handle possibe null pointer exception
	
		if (!wiResource.fields || !wiResource.fields.'System.State') return logResult('no changes made to state')
		
		//if (!stateField) return logResult('not a state change')
		def stateField = wiResource.fields.'System.State'
			
		if (!stateField || stateField == 'null' || stateField == '') return logResult('not a state change')
			
		String oldState = stateField.oldValue
		String newState = stateField.newValue
		
		
		def statechangeCount = wiResource.revision.fields.'Custom.ReOpenCounter'
		
		if (!statechangeCount || statechangeCount == 'null' || statechangeCount == '')
			statechangeCount = 0;
			
		//this is bug id
		String id = "${wiResource.revision.id}"
		
		//this is rev id
		String rev = "${wiResource.rev}"
		
		//parameterize source/destination states for bug count
		if (sourceState.contains(oldState) && (destState.contains(newState))) {
			log.debug("Updating count of $wiType #$id")
	
			if (performIncrementCounter(project, rev, id, statechangeCount, responseHandler)) {
				return logResult('Update Succeeded')
			}
		
			else if (this.retryFailed) {
				log.error('Error updating Custom.ReopenCounter')
				return 'Error updating Custom.ReopenCounter'
			}
					 
		}
		
		else {
			
			return logResult('state change not applicable')
		}
	}

	//Increment Counter would be something like
	private def performIncrementCounter(String project, String rev, def id, def statechangeCount, Closure respHandler = null) {

			int totalCount = statechangeCount + 1;
			
			def data = []
			def t = [op: 'test', path: '/rev', value: rev.toInteger()]
			data.add(t)
			
			//def e = [op: 'add', path: '/fields/Custom.ReOpenCounter', value: 'Active']
			def e = [op: 'add', path: '/fields/Custom.ReOpenCounter', value: totalCount]
			data.add(e)
			this.retryFailed = false
			this.attemptedProject = project
			this.attemptedId = id
			
			return workManagementService.updateWorkItem(collection, project, id, data, respHandler)
			}
		
	private def logResult(def msg) {
		log.debug(msg)
		return msg
	}
}