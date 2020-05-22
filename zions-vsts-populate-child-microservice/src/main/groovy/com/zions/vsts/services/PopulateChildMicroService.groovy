package com.zions.vsts.services

import com.zions.vsts.services.work.WorkManagementService

import com.zions.vsts.services.rmq.mixins.MessageReceiverTrait
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import groovy.json.JsonBuilder

/**
 * Will activate parent work item when child is activated.
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
	
	//@Value('${tfs.types:}')
	@Value('${tfs.types}')
	String wiTypes
	

	public PopulateChildMicroService()
	{
			
	}
	
	 /**
	 * Perform populate child field from parent.
	 *
	 * @see com.zions.vsts.services.ws.client.AbstractWebSocketMicroService#processADOData(java.lang.Object)
	 */
	@Override
	public Object processADOData(Object adoData) {
		log.info("Entering PopulateChild MicroService:: processADOData")
		
		//Uncomment code below to capture adoData payload for test
		/* String json = new JsonBuilder(adoData).toPrettyString()
		 println(json)*/ 
		
		
		def outData = adoData
		def wiResource = adoData.resource
		String wiType = "${wiResource.revision.fields.'System.WorkItemType'}"
		
		// get the OTLNumber * will parameterize later on *
		String otlField = "${wiResource.revision.fields.'Custom.OTLNumber'}"
		
		// Make sure the work items are Component/Epic work items
		if (!wiType && !types.contains(wiType))
		{
			return logResult('not a valid work item type')
		
		}
		// Check to see OTLNumber is populated
		if (!otlField || otlField == 'null' || otlField == '') {
			
			return logResult('field not populated')
			
		}
			
		String project = "${wiResource.revision.fields.'System.TeamProject'}"
		//this is the work item id
		String id = "${wiResource.revision.id}"
		
		//Need to get child data for work item
		//def wi = workManagementService.getWorkItem(collection, project, id)
		def wi = workManagementService.getWorkItem(collection, project, id)
		//should return children payload
		
		def result = workManagementService.getChildren(collection, project, id)
		//def children = result.relations
		
		
		//iterate through the children assigned to work item in question
		def changes = []
		def idMap = [:]
		def count = 0
		//children.each { child ->
		result.each { child ->
			
			def childwi = workManagementService.getWorkItem(child.url)
			
			//Define child work item types
			String type = "${childwi.fields['System.WorkItemType']}"
			//Define OTLNumber of child fields
			String cField = "${childwi.fields.'Custom.OTLNumber'}"
			//get revision id for child - needed to handle concurrency
			String rev = "${childwi.rev}"

			//If child work item is feature or story - update OTLNumber
			
			if ((type == 'Feature' || type == 'User Story') && cField != otlField) {
			
				log.info("Getting the changes for child work item $wiType #$id")
				changes.add(getChanges(project, rev, child, otlField))
				idMap[count] = "${child.id}"
				}
			else {
				
				return logResult('no update needed for this child')
			}
		}
			
		if (changes.size() > 0) {
			// Process work item changes in Azure DevOps
			log.info("Processing work item changes...")
			workManagementService.batchWIChanges(collection, project, changes, idMap)
		}
	}

		
		
		/**			For unit testing !! Uncomment code below to capture parent playload for test
		 * String json = new JsonBuilder(parentWI).toPrettyString()
		 * println(json)*/
		
		
	//define get changes method and create batch call
	private def getChanges(String project, String rev, def child, def otlField) {
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def cid = child.id
		def wiData = [method:'PATCH', uri: "/_apis/wit/workitems/${cid}?api-version=5.0-preview.3&bypassRules=true", headers: ['Content-Type': 'application/json-patch+json'], body: []]
		def idData1 = [op: 'test', path: '/rev', value: rev.toInteger()]
		wiData.body.add(idData1)
		

		// Add work item type in case it changed
		def idData2 = [ op: 'add', path: '/fields/Custom.OTLNumber', value: "$otlField"]
		
		
		wiData.body.add(idData2)
		return wiData
		
	}
	
	private def logResult(def msg)
	{
		log.info(msg)
		return msg
	}
}
