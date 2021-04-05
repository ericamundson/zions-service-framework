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
	
	@Value('${tfs.sourcestate}')
	String sourceState
	
	@Value('${tfs.deststate}')
	String destState
	
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
			Date convCreateDate
			def resolvedDate = bugWI.fields.'Microsoft.VSTS.Common.ResolvedDate'
			Date convResolvedDate
			def daystoCount
			String genPath
			boolean resolveState = false
			boolean closeState = false
			String SysState
			String rev
				
			if (closeState) {
			convClosedDate = Date.parse("yyyy-MM-dd", closedDate)
			def daystoClose = calcManagementService.calcDaysToClose(convClosedDate, convCreateDate)
			daystoCount = daystoClose
			String closedPath = 'Custom.DaysToClose'
			genPath = closedPath
			rev = "${bugWI.rev}"
			}
			
			if (resolveState) {
			convResolvedDate = Date.parse("yyyy-MM-dd", resolvedDate)
			def daystoResolve = calcManagementService.calcDaysToClose(convResolvedDate, convCreateDate)
			daystoCount = daystoResolve
			String resolvedPath = 'Custom.DaysToResolve'
			genPath = resolvedPath
			rev = "${bugWI.rev}"
		}

	
			
			if (performIncrementCounter(this.attemptedProject, rev, genPath, this.attemptedId, daystoCount)) {
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
		def daystoCount
		String genPath
		boolean resolveState = false
		boolean closeState = false
		boolean resetCount = false
		String SysState
		def resetValues = []
		def parentValues = []
		String updField
		def changes = []
		def idMap = [:]
		def count = 0
		
		//**Check for qualifying projects
		String project = "${wiResource.revision.fields.'System.TeamProject'}"
		if (includeProjects && !includeProjects.contains(project))
			return logResult('Project not included')
		
		//get work item id, state and revision
		String id = "${wiResource.revision.id}"
		SysState = "${wiResource.revision.fields.'System.State'}"
		String rev = "${wiResource.rev}"
		//current values for daystoResolve and daystoClose
		def cdaystoResolve = "${wiResource.revision.fields.'Custom.DaysToResolve'}"
		def cdaystoClose = "${wiResource.revision.fields.'Custom.DaysToClose'}"
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
		
		
		//Compare old/new system states
		def stateField = wiResource.fields.'System.State'
		
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
		
		//evaluate state - resolved or closed?
		if (SysState == 'Resolved')
			resolveState = true
		
		if (SysState == 'Closed')
			closeState = true
		
		//from closed to new/active or resolved to new/active - otherwise no change.
		//if (SysState == 'New' || SysState == 'Active')
		if (sourceState.contains(oldState) && (destState.contains(newState)))
			resetCount = true
		else {
			resetCount = false
		}
						
				
		//Get and format closedDate
		def closedDate = wiResource.revision.fields.'Microsoft.VSTS.Common.ClosedDate'
		Date convClosedDate
		
		if (closeState) {
			convClosedDate = Date.parse("yyyy-MM-dd", closedDate)
			def daystoClose = calcManagementService.calcDaysToClose(convClosedDate, convCreateDate)
			daystoCount = daystoClose
			String closedPath = 'Custom.DaysToClose'
			genPath = closedPath
		}

		
		//Get and formate the Resolved Date
		def resolvedDate = wiResource.revision.fields.'Microsoft.VSTS.Common.ResolvedDate'
		Date convResolvedDate
		
		if (resolveState) {
			convResolvedDate = Date.parse("yyyy-MM-dd", resolvedDate)
			def daystoResolve = calcManagementService.calcDaysToClose(convResolvedDate, convCreateDate)
			daystoCount = daystoResolve
			String resolvedPath = 'Custom.DaysToResolve'
			genPath = resolvedPath
		}
		
		if (resetCount) {
			
			if (oldState == "Resolved" || oldState == "Closed" && destState.contains(newState))  {
			
				wiPfields.each { field ->
					def pVal = wiResource.revision.fields["${field}"]
					updField = "${pVal}"
					daystoCount = 0;
					parentValues.add(updField)

					
				}
			
			}
			
		}
		
		if (resetCount == false && closeState == false && resolveState == false)
			return logResult('Changes not applicable')
		
		
		
		/*if (getChanges(project, rev, id, daystoCount, resetValues, responseHandler)) {
			return logResult('Update Succeeded')
		}
		
		if (performIncrementCounter(project, rev, id, daystoCount, genPath, responseHandler)) {
			return logResult('Update Succeeded')
		}*/
		if(resolveState || closeState) {
			log.debug("Updating count of $wiType #$id")
			performIncrementCounter(project, rev, id, daystoCount, genPath, responseHandler)
		}
			
		if(resetCount) {
			log.debug("Resetting count of $wiType #$id")
			changes.add(getChanges(project, rev, id, daystoCount))
			idMap[count] = "${id}"
			
			if (changes.size() > 0) {
				changes.each{change ->
					//capture test data
					//println(change.body.toString())
				}
				// Process work item changes in Azure DevOps
				log.debug("Processing work item changes...")
				workManagementService.batchWIChanges(collection, changes, idMap)
				return logResult('Add Update Succeeded')
			}
			//getChanges(project, rev, id, daystoCount, uField, responseHandler)
		}
	}

		private def performIncrementCounter(String project, String rev, def id, def daystoCount, genPath, Closure respHandler = null) {
			
			String writePath = ("/fields/$genPath")
			
			def data = []
			def t = [op: 'test', path: '/rev', value: rev.toInteger()]
			data.add(t)
			
			//parameterize path: '/fields/Custom.DaysToClose to include DaysToResolve
			def e = [op: 'add', path: writePath, value: daystoCount]
			data.add(e)
			this.retryFailed = false
			this.attemptedProject = project
			this.attemptedId = id
			
			
			return workManagementService.updateWorkItem(collection, project, id, data, respHandler)
		}
		
		//private def getChanges(String project, String rev, def id, def daystoCount, def resetValues, Closure respHandler = null) {
		 
		private def getChanges(String project, String rev, def id, def daystoCount, Closure respHandler = null) {
					
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def wiData = [method:'PATCH', uri: "/_apis/wit/workitems/${id}?api-version=5.0-preview.3&bypassRules=true", headers: ['Content-Type': 'application/json-patch+json'], body: []]
		
		wiData.body.add([op: 'test', path: '/rev', value: rev.toInteger()])
		for(int i=0; i<wiPfields.size(); i++) {
						
			wiData.body.add([ op: 'add', path: "/fields/${wiPfields[i]}", value: daystoCount])
		
		}
	
		return wiData

		//412 retry block
		this.retryFailed = false
		this.attemptedProject = project
		this.attemptedId = id
		return workManagementService.updateWorkItem(collection, project, id, wiData, respHandler)
		}

		private def logResult(def msg) {
			log.debug(msg)
			return msg
		}
		
	}
		

		


