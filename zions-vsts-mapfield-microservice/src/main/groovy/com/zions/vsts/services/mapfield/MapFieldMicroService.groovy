package com.zions.vsts.services.mapfield


import com.zions.vsts.services.asset.SharedAssetService
import com.zions.vsts.services.rmq.mixins.MessageReceiverTrait
import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.rmq.mixins.MessageReceiverTrait

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

/**
 * Will update fields in the Impact value for Workaround based on mapping table:
 *Calculate based on values from Complexity and Frequency:
If Complexity = “Low”, then Impact=“Low”
If (a) Complexity=“Medium” AND (b) Frequency = “Monthly” or “Infrequent”, then Impact = “Medium”
If (a) Complexity= “High” AND (b) Frequency=“Infrequent”, then Impact=“Medium”
If (a) Complexity =“Medium” AND (b) Frequency = “Daily” , then Impact = “High” 
If (a) Complexity =“High” AND (b) Frequency =“Daily” or “Monthly”, then Impact=“High”
 * 
 * 1.	Input Fields (2+).
   ex:  Custom.Frequency, Custom.WorkaroundComplexity
   2.	Output Field
    ex: Custom.Impact

 * @author z070187
 *
 */
@Component
@Slf4j
class MapFieldMicroService implements MessageReceiverTrait {
	@Autowired
	WorkManagementService workManagementService
	
	@Autowired
	ProjectProperties projectProperties
	
	@Autowired
	SharedAssetService sharedAssetService

	@Value('${tfs.collection:}')
	String collection	

	@Value('${tfs.outputMapUID:}')
	String outputMapUID	

	@Autowired
	public MapFieldMicroService() {
	}
	/**
	 * Perform assignment operation
	 * 
	 * @see com.zions.vsts.services.ws.client.AbstractWebSocketMicroService#processADOData(java.lang.Object)
	 */
	@Override
	public Object processADOData(Object adoData) {
//		Uncomment code below to capture adoData payload for test
//		String json = new JsonBuilder(adoData).toPrettyString()
//		println(json)
		def outData = adoData
		def eventType = adoData.eventType
		def wiResource = adoData.resource
		//String id = getRootFieldValue('id', eventType, wiResource)
		String id = "${wiResource.revision.id}"
		String rev = "${wiResource.revision.rev}"
		String project = "${wiResource.revision.fields.'System.TeamProject'}"
		log.debug("Entering MapFieldMicroService:: processADOData <$eventType> #$id")
				
		String wiType = "${wiResource.revision.fields.'System.WorkItemType'}"
		//Make sure work item is Workaround
		if (wiType != 'Workaround') return logResult('Not a Workaround work item')

			
		if (projectProperties.inputFields.size() != 2)
		{
			log.error("Must have two input field values")
			return 'Invalid parameters'
		}
		
		//Get field attributes from YAML file
		String geninput1 =  projectProperties.inputFields[0]
		String geninput2 = projectProperties.inputFields[1]
		String genoutput = projectProperties.outputField
				
				
		if (geninput1 != null && geninput2 != 'null') {
			// Get field map
			String newOutput = lookupOutput(geninput1, geninput2)
			if (genoutput == 'null' || newOutput != genoutput) {
				log.info("Mapping for $wiType #$id")
				try {
					updateOutput(project, id, rev, newOutput)
					return logResult('Output value updated')
				}
				catch (e){
					log.error("Error getting Output: ${e.message}")
					return 'Failed update'
				}
			}
			else return logResult('No updates needed')
		}
		else if (genoutput != 'null'){
			// Need to set map
			updateOutput(project, id, rev, '')
			return logResult('Output set to unassigned')
		}
	}
	

	private def lookupOutput(String geninput1, String geninput2) {
				
		def outputMap = new JsonSlurper().parseText(this.getClass().getResource('/mapping/impactmap.json').text)
		def outputElement = outputMap.find{it.Input1==geninput1 && it.Input2==geninput2}
		//def colorElement = colorMap.find{it.Priority==priority && it.Severity==severity}
				
		return outputElement.Output
		//return colorElement.Color
	}
	private def updateOutput(def project, def id, String rev, String genoutput) {
		def data = []
		def t = [op: 'test', path: '/rev', value: rev.toInteger()]
		data.add(t)
		//def e = [op: 'add', path: '/fields/Custom.Impact', value: output]
		//findout what to pass in output field
		def e = [op: 'add', path: "/fields/${genoutput}", value: genoutput]
		data.add(e)
		workManagementService.updateWorkItem(collection, project, id, data)
	}
	
	private def logResult(def msg) {
		log.debug("Result: $msg")
		return msg
	}
}

