package com.zions.vsts.services

import com.zions.vsts.services.work.WorkManagementService

import com.zions.vsts.services.rmq.mixins.MessageReceiverTrait
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import groovy.json.JsonBuilder

/**
 * Will populate parent field changes to child work items
 *
 * @author z070187
 *
 */
@Component
@Slf4j
class PopulateChildMicroService implements MessageReceiverTrait {

	@Autowired
	WorkManagementService workManagementService
	
	@Value('${tfs.collection:}')
	String collection
	
	@Value('${tfs.types}')
	String[] types
	

	@Value('${tfs.pfield}')
	String wiPfields
	
	
	@Value('${tfs.cfield}')
	String wiCfields

	// Handle HTTP 412 retry when work item revision has changed
	boolean retryFailed
	def attemptedProject
	def attemptedChildId
	def attemptedUpdate
	Closure responseHandler = { resp ->
		
		if (resp.status == 412) {
			
			// Get fresh copy of parent work item
			def childwi = workManagementService.getWorkItem(collection, attemptedProject, attemptedChildId)
			def val = childwi.fields["${wiPfields}"]
			String updField = "${val}"
			String rev = "${childwi.rev}"
	
			if (updField == 'null' || updField == null ) { // Process if still unassigned
				if (getChanges(this.attemptedProject, this.attemptedChildId, rev, this.attemptedUpdate)) {
					return logResult('Work item successfully activated after 412 retry')
				}
				else {
					this.retryFailed = true
					log.error('Failed update after 412 retry')
					return 'Failed update after 412 retry'
				}
			}
		}
	}


	
	public PopulateChildMicroService() {
			
	}
	
	 /**
	 * Perform populate child field from parent.
	 *
	 * @see com.zions.vsts.services.ws.client.AbstractWebSocketMicroService#processADOData(java.lang.Object)
	 */
	@Override
	public Object processADOData(Object adoData) {
		log.info("Entering PopulateChild MicroService:: processADOData")
		
		/*Uncomment code below to capture adoData payload for test*/
		/*String json = new JsonBuilder(adoData).toPrettyString()
		println(json)*/
		
		//def types = wiTypes.split(',')
		def outData = adoData
		def wiResource = adoData.resource
		String wiType = "${wiResource.revision.fields.'System.WorkItemType'}"
		
		//String updField = "${wiResource.revision.fields.'Custom.OTLNumber'}"
		def val = wiResource.revision.fields["${wiPfields}"]
		String updField = "${val}"
		
		//convert null value to ""
		if (!updField || updField == 'null' || updField == '')
			updField = "";

		if (!types.contains(wiType))return logResult('not a valid work item type')

		String project = "${wiResource.revision.fields.'System.TeamProject'}"
		//this is the work item id
		String id = "${wiResource.revision.id}"
		
		//code to address null pointer exception
		if (!wiResource.fields) return logResult('No valid changes made')
	
		//should return children payload - method to mock
		
		def result = workManagementService.getChildren(collection, project, id)
		if (!result || result == 'null' || result == '' || result == []) {
		//println(result.toString())
			return logResult('child not present')
			
		}
		/**	For unit testing !! Uncomment code below to capture child payload for test */
		 /* String json = new JsonBuilder(result).toPrettyString()
		  println(json)*/
		
		 //iterate through the children assigned to work item in question
		def changes = []
		def idMap = [:]
		def count = 0
		result.each { childwi ->
			
			//Define child work item types
			String type = "${childwi.fields['System.WorkItemType']}"
			
			
			//Define OTLNumber of child fields
			// String cField = "${childwi.fields.'Custom.OTLNumber'}"
			
			
			 def val2 = childwi.fields["${wiPfields}"]
			 String cField = "${val2}"
			
			//get revision id for child - needed to handle concurrency
			String rev = "${childwi.rev}"

			//update all child work item types
			if (cField != updField) {
					
				log.info("Getting the changes for child work item $wiType #$id")
				changes.add(getChanges(project, rev, childwi, updField))
				idMap[count] = "${childwi.id}"
				}
	}
			
		if (changes.size() > 0) {
			changes.each{change ->
			//capture test data
			println(change.body.toString())
			}
			// Process work item changes in Azure DevOps
			log.info("Processing work item changes...")
			workManagementService.batchWIChanges(collection, project, changes, idMap)
			return logResult('Update Succeeded')
		}
		else {
			
			return logResult('work item changes do not apply')
		}
	}

	//define get changes method and create batch call
	private def getChanges(String project, String rev, def child, def updField, Closure respHandler = null) {
		
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def cid = child.id
		def wiData = [method:'PATCH', uri: "/_apis/wit/workitems/${cid}?api-version=5.0-preview.3&bypassRules=true", headers: ['Content-Type': 'application/json-patch+json'], body: []]
		def idData1 = [op: 'test', path: '/rev', value: rev.toInteger()]
		wiData.body.add(idData1)
		def idData2 = [ op: 'add', path: "/fields/${wiPfields}", value: "$updField"]
		wiData.body.add(idData2)
		return wiData
		//412 retry block
		this.retryFailed = false
		this.attemptedProject = project
		this.attemptedChildId = child
		this.attemptedUpdate = updField
		return workManagementService.updateWorkItem(collection, project, child, wiData, respHandler)
		
		
	}
	
	private def logResult(def msg)
	{
		log.info(msg)
		return msg
	}
}