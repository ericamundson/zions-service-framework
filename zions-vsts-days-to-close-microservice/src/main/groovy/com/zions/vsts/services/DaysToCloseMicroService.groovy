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
	String[] sourceState
	
	@Value('${tfs.deststate}')
	String[] destState
	
	@Value('${tfs.rsourcestate}')
	String[] rsourceState
	
	@Value('${tfs.rdeststate}')
	String[] rdestState
	
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
			
			String createDate = bugWI.fields.'System.CreatedDate'
			Date convCreateDate = Date.parse("yyyy-MM-dd", createDate);
			
			def resolvedDate = bugWI.fields.'Microsoft.VSTS.Common.ResolvedDate'
			Date convResolvedDate
			Date convClosedDate
			def daystoCount
			String genPath
			String project = bugWI.fields.'System.TeamProject'
			boolean resolveState = false
			boolean closeState = false
			boolean resetCount = false
			boolean resetCloseCount = false
			String SysState = bugWI.fields.'System.State'
			String rev
			String id = "${bugWI.id}"
			def resetValues = []
			def parentValues = []
			String updField
			def changes = []
			def idMap = [:]
			def count = 0
			def stateField = bugWI.fields.'System.State'
			//String oldState = stateField.oldValue
			//String newState = stateField.newValue
			String wiType = bugWI.fields.'System.WorkItemType'
			
			if (SysState == 'Resolved')
				resolveState = true
				
			if (SysState == 'Closed')
				closeState = true
				
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
			
			if (resetCount) {
				
					wiPfields.each { field ->
						def pVal = bugWI.fields["${field}"]
						updField = "${pVal}"
						daystoCount = 0;
						parentValues.add(updField)
					}
				}
			
		
			if (resetCloseCount) {
			
			rev = "${bugWI.rev}"
			daystoCount = 0
			String closedPath = 'Custom.DaysToClose'
			genPath = closedPath
			}

				
			if(resolveState || resetCloseCount) {
				log.debug("Updating count of $wiType #$id")
				performIncrementCounter(project, rev, id, daystoCount, genPath, responseHandler)
			}
				
			if(resetCount || closeState ) {
				log.debug("Setting count of $wiType #$id")
				changes.add(getChanges(project, rev, id, daystoCount))
				(getChanges(this.attemptedProject, rev, this.attemptedId, parentValues))
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
		boolean resetCloseCount = false
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
		
		//resetCloseCount check
		if (oldState == 'Closed' && (newState == 'Resolved'))
		resetCloseCount = true
				
		//set REsolve State True
		if (SysState == 'Resolved')
			resolveState = true
		/*if (oldState == "New" || oldState == "Active" && (newState == "New" || newState == "Active"))
			resolveState = false*/
		
		if (SysState == 'Closed')
			closeState = true
		
		//from closed to new/active or resolved to new/active - otherwise no change.
		if (sourceState.contains(oldState) && (destState.contains(newState)))
			resetCount = true
				
		//Get and format closedDate
		def closedDate = wiResource.revision.fields.'Microsoft.VSTS.Common.ClosedDate'
		Date convClosedDate
		
		if (closeState) {
			
			wiPfields.each { field ->
				convClosedDate = Date.parse("yyyy-MM-dd", closedDate)
				def daystoClose = calcManagementService.calcDaysToClose(convClosedDate, convCreateDate)
				daystoCount = daystoClose
				def pVal = wiResource.revision.fields["${field}"]
				updField = "${pVal}"
				parentValues.add(updField)
		
			}
		
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
		
		if (resetCloseCount) {
	
			daystoCount = 0
			String closedPath = 'Custom.DaysToClose'
			genPath = closedPath
		}
		
		
		if (resetCount == false && closeState == false && resolveState == false && resetCloseCount == false)
			return logResult('Changes not applicable')

		if(resolveState || resetCloseCount) {
			log.debug("Updating count of $wiType #$id")
			performIncrementCounter(project, rev, id, daystoCount, genPath, responseHandler)
		}
			
		if(resetCount || closeState ) {
			log.debug("Setting count of $wiType #$id")
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
			
		}
	}

		private def performIncrementCounter(String project, String rev, def id, def daystoCount, genPath, Closure respHandler = null) {
			
			String writePath = ("/fields/$genPath")
			
			def data = []
			def t = [op: 'test', path: '/rev', value: rev.toInteger()]
			data.add(t)
			
			def e = [op: 'add', path: writePath, value: daystoCount]
			data.add(e)
			this.retryFailed = false
			this.attemptedProject = project
			this.attemptedId = id
			
			
			return workManagementService.updateWorkItem(collection, project, id, data, respHandler)
		}

		 
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
		

		


