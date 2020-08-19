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
class ChangeParentMicroService implements MessageReceiverTrait {

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
	
	@Value('${tfs.project.includes}')
	String[] includeProjects

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
	


	
	public ChangeParentMicroService() {
			
	}
	
	 /**
	 * Remove fields populated by parent when link is removed from child.
	 * Will Also restore field values when a parent is re-attached
	 * @see com.zions.vsts.services.ws.client.AbstractWebSocketMicroService#processADOData(java.lang.Object)
	 */
	@Override
	public Object processADOData(Object adoData) {
		log.info("Entering Change Parent MicroService:: processADOData")
		
		/*Uncomment code below to capture adoData payload for test*/
		//String json = new JsonBuilder(adoData).toPrettyString()
		//println(json)
		
		def outData = adoData
		def wiResource = adoData.resource
		String project = "${wiResource.revision.fields.'System.TeamProject'}"
		if (includeProjects && !includeProjects.contains(project))
			return logResult('Project not included')
		String wiType = "${wiResource.revision.fields.'System.WorkItemType'}"
		if (!types.contains(wiType))return logResult('not a valid work item type')
		def parentValues = []
		def childValues = []
		String updField
		//get parentID
		def parId = "${wiResource.revision.fields.'System.Parent'}"
		def parentWI = workManagementService.getWorkItem(collection, project, parId)
		
		boolean parentAdded = false
		boolean parentRemoved = false
		def changes = []
		def idMap = [:]
		def count = 0
		//this is the work item id
		String id = "${wiResource.revision.id}"
		//Define removed link type
		//def remLinks = "${wiResource.revision.relations['rel']}"
		//code to address null pointer exception
		
	    if (!wiResource.relations) return logResult('No valid changes made')
		  def remLinks = wiResource.relations.removed

		//verify added relations? is .added?
		def addLinks = wiResource.relations.added
		
		if (remLinks) {
			remLinks.each { link ->
				
				 if (link.attributes.name == 'Parent') {
			 	
					 parentRemoved = true
				}
			}
		}
		else if (addLinks) {
			addLinks.each { link ->
				if (link.attributes.name == 'Parent') {
					parentAdded = true
				}
				
			}
		}
		if (!parentRemoved && !parentAdded)
			return logResult('Not a parent link change')
			
		
		//process block for parent link removal
		if (parentRemoved) {
			updField = ""
			String rev = "${wiResource.rev}"
			//reset current fields
			wiPfields.each { field ->
				def cVal = wiResource.revision.fields["${field}"]
				updField = "${cVal}"
				updField = ""
				parentValues.add(updField)
			}
			log.info("Getting the changes for current work item $wiType #$id")
			changes.add(getChanges(project, rev, id, parentValues))
			idMap[count] = "${id}"
			
			if (changes.size() > 0) {
				changes.each{change ->
					//capture test data
					//println(change.body.toString())
				}
				// Process work item changes in Azure DevOps
				log.info("Processing work item changes...")
				workManagementService.batchWIChanges(collection, project, changes, idMap)
				return logResult('Update Succeeded')
			}
		
		}
		//Process block adding a parent
		else {
		
			String rev = "${wiResource.rev}"
			//add values from parent
			
			if (!parentWI){	
			log.error("Error retrieving work item $parId")
			return 'Error Retrieving Parent'
			}
			def pState = parentWI.fields.'System.State'
			wiPfields.each { field ->
				def pVal = parentWI.fields["${field}"]
			//	def pVal = wiResource.revision.fields["${field}"]
				//updField should ultimately reflect the values from parent
				
				updField = "${pVal}"
				if (!updField || updField == 'null' || updField == '')
					updField = "";
					parentValues.add(updField)
			}
			
			log.info("Getting the changes for current work item $wiType #$id")
			changes.add(getChanges(project, rev, id, parentValues))
			idMap[count] = "${id}"
			
			if (changes.size() > 0) {
				changes.each{change ->
					//capture test data
					//println(change.body.toString())
				}
				// Process work item changes in Azure DevOps
				log.info("Processing work item changes...")
				workManagementService.batchWIChanges(collection, project, changes, idMap)
				return logResult('Update Succeeded')
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
		log.info(msg)
		return msg
	}
}

	



