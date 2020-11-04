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
	String[] wiPfields
	
	/*@Value('${tfs.pfield}')
	 String[] wiPfields*/
	
	
	//@Value('${tfs.pfield}')
	//String[] wiPfields
	
	
	@Value('${tfs.cfield}')
	String wiCfields

	// Handle HTTP 412 retry when work item revision has changed
	boolean retryFailed
	def attemptedProject
	def attemptedId
	//def attemptedUpdate
	Closure responseHandler = { resp ->
		
		if (resp.status == 412) {
			
			// Get fresh copy of child work items
			def childwi = workManagementService.getWorkItem(collection, attemptedProject, attemptedId)
			def cid = childwi.id
			String rev = "${childwi.rev}"
			def parentValues = []
			//will need to iterate through all child records to see if fields were populated
			def wiData = [method:'PATCH', uri: "/_apis/wit/workitems/${cid}?api-version=5.0-preview.3&bypassRules=true", headers: ['Content-Type': 'application/json-patch+json'], body: []]
			
			//add data to the body with for loop for number of children under parent
			wiData.body.add([op: 'test', path: '/rev', value: rev.toInteger()])
			for(int i=0; i<wiPfields.size(); i++) {
							
				wiData.body.add([ op: 'add', path: "/fields/${wiPfields[i]}", value: "${parentValues[i]}"])
			}

				if (getChanges(this.attemptedProject, rev, this.attemptedId, parentValues)) {
					return logResult('Work item successfully activated after 412 retry')
				}
				else {
					this.retryFailed = true
					log.error('Failed update after 412 retry')
					return 'Failed update after 412 retry'
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
		log.debug("Entering PopulateChild MicroService:: processADOData")
		
		/*Uncomment code below to capture adoData payload for test*/
		/*String json = new JsonBuilder(adoData).toPrettyString()
		println(json)*/
		
		def outData = adoData
		def wiResource = adoData.resource
		String wiType = "${wiResource.revision.fields.'System.WorkItemType'}"
		def parentValues = []
		def childValues = []
		
		//String updField = "${wiResource.revision.fields.'Custom.OTLNumber'}"
		
		wiPfields.each { field ->
			def val4 = wiResource.revision.fields["${field}"]
			String updField = "${val4}"
			//convert null value to ""
			if (!updField || updField == 'null' || updField == '')
				updField = "";
			
			parentValues.add(updField)

		}
		
		if (!types.contains(wiType))return logResult('not a valid work item type')

		String project = "${wiResource.revision.fields.'System.TeamProject'}"
		//this is the work item id
		String id = "${wiResource.revision.id}"
		if (!id) return logResult('no child found it may have been deleted.')
		
		//code to address null pointer exception
		if (!wiResource.fields) return logResult('No valid changes made')
		
		//should return children payload - method to mock
		def result = workManagementService.getChildren(collection, project, id)
		
		//handle nullpoint here.
		if (!wiResource.fields) return logResult('No valid changes made')
			
		if (!result || result == 'null' || result == '' || result == []) {
		
			return logResult('child not present')
			
		}
		/**	For unit testing !! Uncomment code below to capture child payload for test */
		  /*String json2 = new JsonBuilder(result).toPrettyString()
		  println(json2)*/
		
		
		 //iterate through the children assigned to work item in question
		def changes = []
		def idMap = [:]
		def count = 0
		result.each { childwi ->
			
			//Define child work item types
			String type = "${childwi.fields['System.WorkItemType']}"
	
			//# add each loop for val2 similar to above
			wiPfields.each { field ->
				def val2 = childwi.fields["${field}"]
				String cField = "${val2}"
				childValues.add(cField)
				
			
			}
			
			//get revision id for child - needed to handle concurrency
			boolean childNeedsUpdate = false
			  
			String rev = "${childwi.rev}"
			for(int i=0; i<wiPfields.size(); i++) {
				if (childValues[i] != parentValues[i]) childNeedsUpdate = true
					else log.debug("no update needed")
			}
			
			if (childNeedsUpdate) {
				log.debug("Getting the changes for child work item $wiType #$id")
			
				changes.add(getChanges(project, rev, childwi, parentValues))
				idMap[count] = "${childwi.id}"
			}
			

		}
			
			
			
		if (changes.size() > 0) {
			changes.each{change ->
			//capture test data
			//println(change.body.toString())
			}
			// Process work item changes in Azure DevOps
			log.debug("Processing work item changes...")
			workManagementService.batchWIChanges(collection, changes, idMap)
			return logResult('Update Succeeded')
		}
		else {
			
			return logResult('work item changes do not apply')
		}
	}


	//pass parentValues to getChanges
	private def getChanges(String project, String rev, def child, def parentValues, Closure respHandler = null) {

		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def cid = child.id
		
		def wiData = [method:'PATCH', uri: "/_apis/wit/workitems/${cid}?api-version=5.0-preview.3&bypassRules=true", headers: ['Content-Type': 'application/json-patch+json'], body: []]
		
		wiData.body.add([op: 'test', path: '/rev', value: rev.toInteger()])
		for(int i=0; i<wiPfields.size(); i++) {
						
			wiData.body.add([ op: 'add', path: "/fields/${wiPfields[i]}", value: "${parentValues[i]}"])
		}
		

		return wiData

		//412 retry block
		this.retryFailed = false
		this.attemptedProject = project
		this.attemptedId = child
		return workManagementService.updateWorkItem(collection, project, child, wiData, respHandler)
		
		
	}
	
	private def logResult(def msg)
	{
		log.debug(msg)
		return msg
	}
}