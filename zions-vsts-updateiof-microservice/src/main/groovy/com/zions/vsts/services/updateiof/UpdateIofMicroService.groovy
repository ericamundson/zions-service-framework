package com.zions.vsts.services.updateiof


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
 * Will update fields in the IOF work item based on mapping table:
 *
 * 
 * @author z070187
 *
 */
@Component
@Slf4j
class UpdateIofMicroService implements MessageReceiverTrait {
	@Autowired
	WorkManagementService workManagementService
	
	@Autowired
	ProjectConfig projectConfig
	
	@Autowired
	SharedAssetService sharedAssetService

	@Value('${tfs.collection:}')
	String collection	

	@Value('${tfs.colorMapUID:}')
	String colorMapUID	

	@Autowired
	public UpdateIofMicroService() {
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
		String id = getRootFieldValue('id', eventType, wiResource)
		log.debug("Entering UpdateIofMicroService:: processADOData <$eventType> #$id")
		
		String wiType = getFieldValue('System.WorkItemType', eventType, wiResource)
		
		//Make sure work item is IO Entity
		if (wiType != 'IO Entity') return logResult('Not a IO Entity work item')
		
		//define IO Entity fields to map
		//Integer priority = getFieldValue('Microsoft.VSTS.Common.Priority', eventType, wiResource)
		String severity = getFieldValue('Microsoft.VSTS.Common.Severity', eventType, wiResource)
		
		//define IO Entity IO Entity Type
		String IoEntType = getFieldValue('Custom.IOEntityType', eventType, wiResource)
		
		//define TransactionNumber
		String TransNbr = getFieldValue('Custom.TransactionNum', eventType, wiResource)
		
		
		//iterate through YAML configuration
		
		String project = getFieldValue('System.TeamProject', eventType, wiResource)
		
		def config
		def defaultConfig
		def projectConfigs = projectConfig.projects
		projectConfigs.each { projectConfig ->
			def name = projectConfig.name
			
			if (project == name) {
				config = projectConfig
				return
			}
			if (name == 'default')	{
				defaultConfig = projectConfig
			}
		}
		if (!config)
			config = defaultConfig
		
		
		String rev = getRootFieldValue('rev', eventType, wiResource)
		
		if (IoEntType != null && TransNbr != 'null') {
			// Get field mapping
			String newField = lookupField(IoEntType, TransNbr)
			if (color == 'null' || newColor != color) {
				log.info("Updating color for $wiType #$id")
				try {
					updateColor(project, id, rev, newColor)
					return logResult('Color updated')
				}
				catch (e){
					log.error("Error updating Custom.Color: ${e.message}")
					return 'Failed update'
				}
			}
			else return logResult('No updates needed')
		}
		else if (color != 'null'){
			// Need to set color to unassigned
			updateColor(project, id, rev, '')
			return logResult('Color set to unassigned')
		}
	}
	private def getFieldValue(def field, def eventType, def wiResource) {
		def value
		if (eventType == 'workitem.created') {
			value = wiResource.fields[field]
		} else {
			value = wiResource.revision.fields[field]
		}
		if (field == 'Microsoft.VSTS.Common.Priority') {
			if (value == null) return null
			return "$value".toInteger()
		} else {
			return "$value"
		}
	}
	private def getRootFieldValue(def field, def eventType, def wiResource) {
		def value
		if (eventType == 'workitem.created') {
			value = wiResource[field]
		} else {
			value = wiResource.revision[field]
		}
		return "$value"
	}
	private def lookupField(String IoEntType, String TransNbr) {
		def colorMap = sharedAssetService.getAsset(collection, colorMapUID)
		def colorElement = colorMap.find{it.Priority==priority && it.Severity==severity}
		return colorElement.Color
	}
	private def updateColor(def project, def id, String rev, String color) {
		def data = []
		def t = [op: 'test', path: '/rev', value: rev.toInteger()]
		data.add(t)
		def e = [op: 'add', path: '/fields/Custom.Color', value: color]
		data.add(e)
		workManagementService.updateWorkItem(collection, project, id, data)
	}
	
	private def logResult(def msg) {
		log.debug("Result: $msg")
		return msg
	}
}

