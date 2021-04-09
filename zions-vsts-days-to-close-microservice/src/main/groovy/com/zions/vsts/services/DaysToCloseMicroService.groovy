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
	
	@Value('${tfs.pfield}')
	String[] wiPfields
	
	@Value('${tfs.closedstate}')
	String[] closedState
	
	@Value('${tfs.openstate}')
	String[] openState
	
	
	// Handle HTTP 412 retry when work item revision has changed
	boolean retryFailed
	def attemptedProject
	def attemptedId
	Date attemptedCreateDate
	Date attemptedCloseDate
	String createDate
	String closedDate
	String resolvedDate
	Date convCreateDate
	Date convClosedDate
	Date convResolvedDate
	boolean resolveOnly = false
	boolean resetCloseCount = false
	boolean updateBoth = false
	boolean resetCount = false
	boolean resolveToClose = false
	boolean resetResolve = false
	def stateField
	int daystoResolve
	int daystoClose
	
	Closure responseHandler = { resp ->
		
		if (resp.status == 412) {
			
			// Get fresh copy of parent work item
			def bugWI = workManagementService.getWorkItem(collection, attemptedProject, attemptedId)
			String rev = bugWI.rev
			String bugState = bugWI.fields.'System.State'
			String oldState = stateField
			String oldState2 = stateField.newValue
			if (bugState != oldState2)  {
			//recheck calculations and boolean conditions
				if (oldState2 == 'Resolved' && (bugState == 'Closed'))
					resolveToClose = true
				
				//resetCloseCount check
				if (oldState2 == 'Closed' && (bugState == 'Resolved'))
				resetCloseCount = true
						
				//New/Active to Resolve reset DaystoResolve only
				if (openState.contains(oldState2) && (bugState == 'Resolved'))
					resolveOnly = true
					if (resolveOnly) {
						resolvedDate = bugWI.fields.'Microsoft.VSTS.Common.ResolvedDate'
						convResolvedDate = Date.parse("yyyy-MM-dd", resolvedDate)
						daystoResolve = calcManagementService.calcDaysToClose(convResolvedDate, convCreateDate)
					}
					
				//set back to New/Active from Resolved 
				if (openState.contains(bugState) && (oldState2 == 'Resolved'))
					resetResolve = true
					
					//set back to New/Active from Resolved
				if (openState.contains(bugState) && (oldState2 == 'Closed'))
					resetCount = true
					
					//update both daysToResolve and daysToClose
				if (openState.contains(oldState2) && (bugState == 'Closed'))
					updateBoth = true
				
				
			}
			
			if (performIncrementCounter(this.attemptedProject, rev, this.attemptedId, daystoResolve, daystoClose)) {
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
		
		def wiResource = adoData.resource

		
		//**Check for qualifying projects
		String project = "${wiResource.revision.fields.'System.TeamProject'}"
		if (includeProjects && !includeProjects.contains(project))
			return logResult('Project not included')
		
		//get work item id, state and revision
		String id = "${wiResource.revision.id}"
		String rev = "${wiResource.rev}"
		String wiType = "${wiResource.revision.fields.'System.WorkItemType'}"
		
		//If Bug execute counter code
		if (!types.contains(wiType))return logResult('not a valid work item type')
		
		//NPE check
		if (!wiResource.fields || !wiResource.fields.'System.State') return logResult('no changes made to state')
			
		//Get Created Date
		createDate = "${wiResource.revision.fields.'System.CreatedDate'}"
		if (!createDate) {
			log.error("Error retrieving create date for work item $id")
			return 'Error Retrieving Create Date'
		}
		//Format the Created Date
		convCreateDate = Date.parse("yyyy-MM-dd", createDate);
		this.attemptedCreateDate = convCreateDate
			
		//Compare old/new system states
		stateField = wiResource.fields.'System.State'
		
		if (!stateField || stateField == 'null' || stateField == '') return logResult('not a state change')
		//restrieve old/new state field values for comparison
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
		
	//Leave DaysToResolve only update DaysToClose - CloseState = true
		if (oldState == 'Resolved' && (newState == 'Closed'))
			resolveToClose = true
		
		//resetCloseCount check
		if (oldState == 'Closed' && (newState == 'Resolved'))
		resetCloseCount = true
				
		//New/Active to Resolve reset DaystoResolve only
		if (openState.contains(oldState) && (newState == 'Resolved'))
			
			resolveOnly = true
			
		//if new state is open/active and olds state was Resolved
		
		if (openState.contains(newState) && (oldState == 'Resolved'))
			resetResolve = true

		//update both daysToResolve and daysToClose
		if (openState.contains(oldState) && (newState == 'Closed'))
			updateBoth = true

		//from closed to new/active reset count on both daystoResolve and daystoClose
		if (openState.contains(newState) && (oldState == 'Closed'))
			resetCount = true
		

		//only update DaysToClose
		if (resolveToClose) {
			
			closedDate = wiResource.revision.fields.'Microsoft.VSTS.Common.ClosedDate'
			convClosedDate = Date.parse("yyyy-MM-dd", closedDate)
			daystoClose = calcManagementService.calcDaysToClose(convClosedDate, convCreateDate)
			log.debug("Updating count of $wiType #$id")
			performIncrementCounter(project, rev, id, daystoResolve, daystoClose, responseHandler)
		
		}

		//update both values
		if (updateBoth) {
			resolvedDate = wiResource.revision.fields.'Microsoft.VSTS.Common.ResolvedDate'
			convResolvedDate = Date.parse("yyyy-MM-dd", resolvedDate)
			daystoResolve = calcManagementService.calcDaysToClose(convResolvedDate, convCreateDate)
			closedDate = wiResource.revision.fields.'Microsoft.VSTS.Common.ClosedDate'
			convClosedDate = Date.parse("yyyy-MM-dd", closedDate)
			daystoClose = calcManagementService.calcDaysToClose(convClosedDate, convCreateDate)
			log.debug("Updating count of $wiType #$id")
			performIncrementCounter(project, rev, id, daystoResolve, daystoClose, responseHandler)
			
		}
		
		
		//only update DaysToResolve.
		if (resolveOnly) {
			resolvedDate = wiResource.revision.fields.'Microsoft.VSTS.Common.ResolvedDate'
			convResolvedDate = Date.parse("yyyy-MM-dd", resolvedDate)
			daystoResolve = calcManagementService.calcDaysToClose(convResolvedDate, convCreateDate)
			log.debug("Updating count of $wiType #$id")
			performIncrementCounter(project, rev, id, daystoResolve, daystoClose, responseHandler)
			
		}
		
		//reset both DaysToResolve and DaysToClose
		if (resetCount) {
			daystoResolve = 0
			daystoClose = 0
			log.debug("Resetting count of $wiType #$id")
			performIncrementCounter(project, rev, id, daystoResolve, daystoClose, responseHandler)
		}
		
		//reset only DaysToClose
		if (resetCloseCount) {
	
			daystoClose = 0
			log.debug("Resetting count of $wiType #$id")
			performIncrementCounter(project, rev, id, daystoResolve, daystoClose, responseHandler)
		}
		
		if (resetResolve) {
			
			daystoResolve = 0
			daystoClose == null 
			log.debug("Resetting count of $wiType #$id")
			performIncrementCounter(project, rev, id, daystoResolve, daystoClose, responseHandler)
				}
		
		
		if (resetCount == false && updateBoth == false && resolveOnly == false && resetCloseCount == false)
			return logResult('Changes not applicable')


	}

			private def performIncrementCounter(String project, String rev, def id, daystoResolve, daystoClose, Closure respHandler = null) {
			
				
			//If daystoClose || daystoResolve == null - only update one item			
			def data = []
			def t = [op: 'test', path: '/rev', value: rev.toInteger()]
			data.add(t)
			
			if (!daystoClose || daystoClose == 'null' || daystoClose == '') {
				def e = [op: 'add', path: '/fields/Custom.DaysToResolve', value: daystoResolve]
				data.add(e)
			}
			
			if (!daystoResolve || daystoResolve == 'null' || daystoResolve == '') {
			 	//if (fields == null || update)
				def e2 = [op: 'add', path: '/fields/Custom.DaysToClose', value: daystoClose]
				data.add(e2)
			}
			
			if (daystoClose || !daystoClose == 'null' || !daystoClose == '' && daystoResolve || !daystoResolve == 'null' || !daystoResolve == '') {
				def e3 = [op: 'add', path: '/fields/Custom.DaysToResolve', value: daystoResolve]
				data.add(e3)
				
				def e4 = [op: 'add', path: '/fields/Custom.DaysToClose', value: daystoClose]
				data.add(e4)
				
			}
			
			
			this.retryFailed = false
			this.attemptedProject = project
			this.attemptedId = id
			
			println(data)
			return workManagementService.updateWorkItem(collection, project, id, data, respHandler)
		}

	private def logResult(def msg) {
			log.debug(msg)
			return msg
		}
		
	}
		

		


