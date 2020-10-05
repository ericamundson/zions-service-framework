package com.zions.vsts.services

															  
import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.rmq.mixins.MessageReceiverTrait
import com.zions.vsts.services.ws.client.AbstractWebSocketMicroService
import groovy.util.logging.Slf4j
import groovy.time.TimeCategory
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.TimeZone
import java.text.ParseException;
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import groovy.json.JsonBuilder




/**
 * Will count number of days taken to close bugs
  *
 * @author z070187
 *
 */

@Component
@Slf4j
class DaysToCloseMicroService implements MessageReceiverTrait {

	@Autowired
	WorkManagementService workManagementService
	
	@Value('${tfs.collection:}')
	String collection

	@Value('${tfs.types}')
	String[] types

	@Value('${tfs.sourcestates}')
	String sourceStates
	
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
			def daystoClose = bugWI.fields.'Custom.DaysToClose'
			String rev = "${bugWI.rev}"
			
			if (!daystoClose|| daystoClose == 'null' || daystoClose == '')
				daystoClose = 0;
	
			
			if (performIncrementCounter(this.attemptedProject, rev, this.attemptedId, daystoClose)) {
				return logResult('Work item successfully counted after 412 retry')
			}
			
			else {
				this.retryFailed = true
				log.error('Failed update after 412 retry')
				return 'Failed update after 412 retry'
			}
			
		}
	}

	
	public DaysToCloseMicroService(){
			
	}
	
	 /**
	 * Perform parent activation on state changes for Task work items.
	 *
	 * @see com.zions.vsts.services.ws.client.AbstractWebSocketMicroService#processADOData(java.lang.Object)
	 */
	@Override
	public Object processADOData(Object adoData) {
		log.info("Entering DaysToCloseMicroService:: processADOData")
		
		/**		Uncomment code below to capture adoData payload for test*/
		 /*String json = new JsonBuilder(adoData).toPrettyString()
		 println(json)*/
		
		def outData = adoData
		def wiResource = adoData.resource
		
		
		//**Check for qualifying projects
		String project = "${wiResource.revision.fields.'System.TeamProject'}"
		/*if (includeProjects && !includeProjects.contains(project))
			return logResult('Project not included')*/
		
		//get work item id and revision
		String id = "${wiResource.revision.id}"
		String rev = "${wiResource.rev}"
		
		//detect the work item type involved in the edit
		String wiType = "${wiResource.revision.fields.'System.WorkItemType'}"
		
		//If Bug execute counter code
		if (!types.contains(wiType))return logResult('not a valid work item type')
		
		//NPE check
		if (!wiResource.fields || !wiResource.fields.'System.State') return logResult('no changes made to state')
		
		def stateField = wiResource.fields.'System.State'
		if (!stateField || stateField == 'null' || stateField == '') return logResult('not a state change')
		
		//retrieve old/new state field values for comparison
		String oldState = stateField.oldValue
		if (!oldState || oldState == 'null') {
			log.error("Error retrieving previous state for work item $id")
			return 'Error Retrieving previous state'
		}
		String newState = stateField.newValue
		if (!newState || newState == 'null') {
			log.error("Error retrieving new state for work item $id")
			return 'Error Retrieving new state state'
		}
		//define days to close and initialize
		def daystoClose = wiResource.revision.fields.'Custom.DaysToClose'
			
		//if daystoClose is null initialize to 0
		if (!daystoClose|| daystoClose == 'null' || daystoClose == '')
			daystoClose = 0;
			

		
		//Get Created Date
		String createDate = "${wiResource.revision.fields.'System.CreatedDate'}"
		if (!createDate) {
			log.error("Error retrieving create date for work item $id")
			return 'Error Retrieving Create Date'
		}
		//Format the Created Date
		Date convCreateDate = Date.parse("yyyy-MM-dd", createDate);
		createDate = convCreateDate.format('dd-MMM-yyyy')
		

		//If Bug is opened reset the count
		if (sourceStates.contains(newState) && (destState.contains(oldState))) {
			daystoClose = 0;
			log.info("Updating count of $wiType #$id")
			//update the daystoClose field
			if (performIncrementCounter(project, rev, id, daystoClose, responseHandler)) {
				return logResult('Update Succeeded')
		}
		
		} else {
		
			 
			log.info("Updating count of $wiType #$id")
			
			//Get and format closedDate
			String closedDate = wiResource.fields.'Microsoft.VSTS.Common.ClosedDate'
			if (!closedDate) {
				log.error("Error retrieving closed date for work item $id")
				return 'Error Retrieving Closed Date'
			}
			String newClosedDate = closedDate.newValue
			Date convClosedDate = Date.parse("yyyy-MM-dd", newClosedDate);
			newClosedDate = convClosedDate.format('dd-MMM-yyyy')
			
			//Determine number of days between created and closed dates
			def duration = groovy.time.TimeCategory.minus(
				new Date(newClosedDate),
				new Date(createDate)
			  );
			  
			def values = duration.days
			daystoClose = values
			//set half day values to 0.5
			if (createDate == newClosedDate) {
				float sameDayClosure
				sameDayClosure = 0.5
				values = sameDayClosure
				daystoClose = values
					
			}
				//update the daystoClose field
		if (performIncrementCounter(project, rev, id, daystoClose, responseHandler)) {
			return logResult('Update Succeeded')
		}
	}
}
				

		//Set number of days between created and closed dates
		private def performIncrementCounter(String project, String rev, def id, def daystoClose, Closure respHandler = null) {
	
				def data = []
				def t = [op: 'test', path: '/rev', value: rev.toInteger()]
				data.add(t)
				
				def e = [op: 'add', path: '/fields/Custom.DaysToClose', value: daystoClose]
				data.add(e)
				this.retryFailed = false
				this.attemptedProject = project
				this.attemptedId = id
				
				return workManagementService.updateWorkItem(collection, project, id, data, respHandler)
		}
		
		
				
	
					
			
		private def logResult(def msg) {
			log.info(msg)
			return msg
		}
		
	}
			
	
		


