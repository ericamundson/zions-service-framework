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
class ParentChangeMicroService implements MessageReceiverTrait {

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
			def currWI = workManagementService.getWorkItem(collection, attemptedProject, attemptedId)
			def cid = currWI.id
			String rev = "${currWI.rev}"
			
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
	

	public ParentChangeMicroService() {
			
	}
	
	 /**
	 * Remove fields populated by parent when link is removed from child.
	 * Will Also restore field values when a parent is re-attached
	 * @see com.zions.vsts.services.ws.client.AbstractWebSocketMicroService#processADOData(java.lang.Object)
	 */
	@Override
	public Object processADOData(Object adoData) {
		log.debug("Entering Change Parent MicroService:: processADOData")
		
		/*Uncomment code below to capture adoData payload for test*/
		/*String json = new JsonBuilder(adoData).toPrettyString()
		println(json)*/
		
		def outData = adoData
		def wiResource = adoData.resource
		String project = "${wiResource.revision.fields.'System.TeamProject'}"
		//if (includeProjects && !includeProjects.contains(project))
			//return logResult('Project not included')
		String wiType = "${wiResource.revision.fields.'System.WorkItemType'}"
		if (!types.contains(wiType))return logResult('not a valid work item type')
		def parentValues = []
		def childValues = []
		String updField
		//get parentID
		def parId = "${wiResource.revision.fields.'System.Parent'}"
		def parentWI = workManagementService.getWorkItem(collection, project, parId)
		/**	 For unit testing !! Uncomment code below to capture parent playload for test*/
		/* String json2 = new JsonBuilder(parentWI).toPrettyString()
		 println(json2)*/
			
		boolean parentAdded = false
		boolean parentRemoved = false
		boolean childAdded = false
		boolean childRemoved = false
		String curl
		String cid
		String crev
		String rev
		def result
		def changes = []
		def idMap = [:]
		def count = 0
		//this is the work item id
		String id = "${wiResource.revision.id}"
	
		//code to address null pointer exception on 'removed' object
		if (wiResource.relations == 'null' || !wiResource.relations)
			return logResult('No valid changes made') 

		//Define removed link type
		def remLinks = wiResource.relations.removed

		//Define added link type
		def addLinks = wiResource.relations.added
		
		//if (!wiResource.relations) return logResult('No valid changes made')
		if (addLinks == 'null' || remLinks == 'null') return logResult('No valid link changes made')
		
		if (remLinks) {
			remLinks.each { link ->
				
				 if (link.attributes.name == 'Parent') {
			 	
					 parentRemoved = true
				} else {
					 childRemoved = true
					 curl = link.url
					 println(curl)
					 
				}
			}
		}
		else if (addLinks) {
			addLinks.each { link ->
				if (link.attributes.name == 'Parent') {
					parentAdded = true
				} else {
					childAdded = true
					curl = link.url
					println(curl)
				}
				
			}
		}
		if (!parentRemoved && !parentAdded && !childRemoved && !childAdded)
			return logResult('Not a parent link change')
			
		//process block for parent/child link removal
		//Set OTLNumber to blank on the child
		rev = "${wiResource.rev}"
		if (childRemoved) {
			updField = ""
			//get work item info for child, id and revision number
			result = workManagementService.getWorkItem(curl)
			if (!result || result == 'null' || result == '' || result == []) {
				
				return logResult('child not present')
			}
			crev = "${result.rev}"
			cid = "${result.id}"
			id = cid
			rev = crev
		}
		else if (parentRemoved) {
			//reset current fields
			wiPfields.each { field ->
				def cVal = wiResource.revision.fields["${field}"]
				updField = "${cVal}"
				updField = ""
				parentValues.add(updField)
			}
			
			
			log.debug("Getting the changes for current work item $wiType #$id")
			changes.add(getChanges(project, rev, id, parentValues))
			idMap[count] = "${id}"
			
			if (changes.size() > 0) {
				changes.each{change ->
					//capture test data
					//println(change.body.toString())
				}
				// Process work item changes in Azure DevOps
				log.debug("Processing work item changes...")
				workManagementService.batchWIChanges(collection, changes, idMap)
				return logResult('Remove Update Succeeded')
			}
		
		}
		//Process block adding a parent/child and updating the child's OTLNumber
		else {
		if (childAdded) {
			//get work item info for child, id and revision number
			result = workManagementService.getWorkItem(curl)
			if (!result || result == 'null' || result == '' || result == []) {
				
				return logResult('child not present')
			}
			crev = "${result.rev}"
			cid = "${result.id}"
			id = cid
			rev = crev
		}			
		
		else if (parentAdded) {
			//reset current fields
			wiPfields.each { field ->
				//def cVal = wiResource.revision.fields["${field}"]
				def pVal = parentWI.fields["${field}"]
				updField = "${pVal}"
				if (!updField || updField == 'null' || updField == '')
					updField = "";
					parentValues.add(updField)

			}
		}
		
			//add values from parent
			
			if (!parentWI){	
			log.error("Error retrieving work item $parId")
			return 'Error Retrieving Parent'
			}
			
			wiPfields.each { field ->
				def pVal = parentWI.fields["${field}"]
				updField = "${pVal}"
				if (!updField || updField == 'null' || updField == '')
					updField = "";
					parentValues.add(updField)

					
			}
			
			log.debug("Getting the changes for current work item $wiType #$id")
			changes.add(getChanges(project, rev, id, parentValues))
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
	//pass parentValues to getChanges
	private def getChanges(String project, String rev, def id, def parentValues, Closure respHandler = null) {

		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def cid = id
		
		def wiData = [method:'PATCH', uri: "/_apis/wit/workitems/${cid}?api-version=5.0-preview.3&bypassRules=true", headers: ['Content-Type': 'application/json-patch+json'], body: []]
		
		wiData.body.add([op: 'test', path: '/rev', value: rev.toInteger()])
		for(int i=0; i<wiPfields.size(); i++) {
						
			wiData.body.add([ op: 'add', path: "/fields/${wiPfields[i]}", value: "${parentValues[i]}"])
		
		}
	
		return wiData

		//412 retry block
		this.retryFailed = false
		this.attemptedProject = project
		this.attemptedId = id
		return workManagementService.updateWorkItem(collection, project, id, wiData, respHandler)
	}
	
	private def logResult(def msg)
	{
		log.debug(msg)
		return msg
	}
}

	



