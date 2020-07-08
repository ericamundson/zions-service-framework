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
	def attemptedBugId
	def attemptedCount
	Closure responseHandler = { resp ->
		
		if (resp.status == 412) {
			
			// Get fresh copy of parent work item
			def bugWI = workManagementService.getWorkItem(collection, attemptedProject, attemptedBugId)
			String rev = "${bugWI.rev}"
			
			//ado event structure different from work item structure - bugWI.fields represents work item structure
			def statechangeCount = bugWI.fields.'Custom.ReOpenCounter'
			
			if (!statechangeCount || statechangeCount == 'null' || statechangeCount == '')
					statechangeCount = 0;
	
			if (performIncrementCounter(this.attemptedProject, this.attemptedBugId, rev, statechangeCount)) {
				return logResult('Work item successfully activated after 412 retry')
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
		log.info("Entering StateChangeCounterMicroService:: processADOData")
		
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
		//define a variable to get just fields System.State
		def stateField = wiResource.fields.'System.State'
		
		if (!stateField) return logResult('not a state change')
		
		String oldState = stateField.oldValue
		String newState = stateField.newValue
		
		def statechangeCount = wiResource.revision.fields.'Custom.ReOpenCounter'
		
		if (!statechangeCount || statechangeCount == 'null' || statechangeCount == '')
			statechangeCount = 0;
			
		if (!types.contains(wiType))return logResult('not a valid work item type')
		//handle possibe null pointer exception	
		if (!wiResource.fields || !wiResource.fields.'System.State') return logResult('Work item not changed or closed')
		
		//this is bug id
		String id = "${wiResource.revision.id}"
		
		//this is rev id
		String rev = "${wiResource.rev}"
		
		//parameterize source/destination states for bug count
		if (sourceState.contains(oldState) && (destState.contains(newState))) {
			log.info("Updating count of $wiType #$id")
	
			if (performIncrementCounter(project, rev, id, statechangeCount, responseHandler)) {
				return logResult('Update Succeeded')
			}
		
			else if (this.retryFailed) {
				log.error('Error updating Custom.ReopenCounter')
				return 'Error updating Custom.ReopenCounter'
			}
					 
			else {
						 
				return logResult('will not be counted')
			}
		}
		
		else {
			
			return logResult('state change not applicable')
		}
	}

	//Increment Counter would be something like
	private def performIncrementCounter(String project, String rev, def bugId, def statechangeCount, Closure respHandler = null) {

			int totalCount = statechangeCount + 1;
			
			def data = []
			def t = [op: 'test', path: '/rev', value: rev.toInteger()]
			data.add(t)
			
			//def e = [op: 'add', path: '/fields/Custom.ReOpenCounter', value: 'Active']
			def e = [op: 'add', path: '/fields/Custom.ReOpenCounter', value: totalCount]
			data.add(e)
			this.retryFailed = false
			this.attemptedProject = project
			this.attemptedBugId = bugId
			this.attemptedCount = statechangeCount
			return workManagementService.updateWorkItem(collection, project, bugId, data, respHandler)
			}
		
	private def logResult(def msg) {
		log.info(msg)
		return msg
	}
}