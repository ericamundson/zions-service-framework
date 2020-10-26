package com.zions.vsts.services

															  
import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.work.calculations.CalculationManagementService
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
	
	@Autowired
	CalculationManagementService calcManagementService
	
	@Value('${tfs.collection:}')
	String collection

	@Value('${tfs.types}')
	String[] types

	@Value('${tfs.project.includes}')
	String[] includeProjects
	
	// Handle HTTP 412 retry when work item revision has changed
	boolean retryFailed
	def attemptedProject
	def attemptedId
	Date attemptedCreateDate
	
	Closure responseHandler = { resp ->
		
		if (resp.status == 412) {
			
			// Get fresh copy of parent work item
			def bugWI = workManagementService.getWorkItem(collection, attemptedProject, attemptedId)
			def closedDate = bugWI.fields.'Microsoft.VSTS.Common.ClosedDate'
			Date convClosedDate
			
			if (closedDate) {
				convClosedDate = Date.parse("yyyy-MM-dd", closedDate)
			}
			
			def daystoClose = calcManagementService.calcDaysToClose(convClosedDate, attemptedCreateDate)

			String rev = "${bugWI.rev}"
			
			
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
		log.debug("Entering DaysToCloseMicroService:: processADOData")
		
		/**		Uncomment code below to capture adoData payload for test*/
		 /*String json = new JsonBuilder(adoData).toPrettyString()
		 println(json)*/
		
		def outData = adoData
		def wiResource = adoData.resource
		
		
		//**Check for qualifying projects
		String project = "${wiResource.revision.fields.'System.TeamProject'}"
		if (includeProjects && !includeProjects.contains(project))
			return logResult('Project not included')
		
		//get work item id and revision
		String id = "${wiResource.revision.id}"
		String rev = "${wiResource.rev}"
		
		//detect the work item type involved in the edit
		String wiType = "${wiResource.revision.fields.'System.WorkItemType'}"
		
		//If Bug execute counter code
		if (!types.contains(wiType))return logResult('not a valid work item type')
		
		//NPE check
		if (!wiResource.fields || !wiResource.fields.'System.State') return logResult('no changes made to state')
		
		
		//Get Created Date
		String createDate = "${wiResource.revision.fields.'System.CreatedDate'}"
		if (!createDate) {
			log.error("Error retrieving create date for work item $id")
			return 'Error Retrieving Create Date'
		}
		//Format the Created Date
		Date convCreateDate = Date.parse("yyyy-MM-dd", createDate);
		this.attemptedCreateDate = convCreateDate
		//Get and format closedDate
		
		def closedDate = wiResource.revision.fields.'Microsoft.VSTS.Common.ClosedDate'
		Date convClosedDate 
		
		if (closedDate) {
			convClosedDate = Date.parse("yyyy-MM-dd", closedDate)
		}
		
		def daystoClose = calcManagementService.calcDaysToClose(convClosedDate, convCreateDate)
	
		
		log.debug("Updating count of $wiType #$id")
		if (performIncrementCounter(project, rev, id, daystoClose, responseHandler)) {
			return logResult('Update Succeeded')
		}
	}

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
			log.debug(msg)
			return msg
		}
		
}
			
	
		


