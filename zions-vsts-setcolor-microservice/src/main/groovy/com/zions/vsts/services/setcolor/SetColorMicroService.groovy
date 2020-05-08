package com.zions.vsts.services.setcolor

import com.zions.vsts.services.asset.SharedAssetService
import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.ws.client.AbstractWebSocketMicroService

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

/**
 * Validates and, if necessary, updates the Color field on a Bug when any of the following field values have changed:
 * <ul>
 * <li>Priority</li>
 * <li>Severity</li>
 * <li>Color</li>
 * </ul>
 * 
 * @author z097331
 *
 */
@Component
@Slf4j
class SetColorMicroService extends AbstractWebSocketMicroService {
	@Autowired
	WorkManagementService workManagementService

	@Autowired
	SharedAssetService sharedAssetService

	@Value('${tfs.collection:}')
	String collection	

	@Value('${tfs.colorMapUID:}')
	String colorMapUID	

	@Value('${websocket.topic}')
    private String eventTopic

	@Autowired
	public SetColorMicroService(@Value('${websocket.url:}') websocketUrl, 
		@Value('${websocket.user:#{null}}') websocketUser,
		@Value('${websocket.password:#{null}}') websocketPassword) {
		super(websocketUrl, websocketUser, websocketPassword)
	}
	public SetColorMicroService() {
		// Constructor for unit testing
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
		log.info("Entering SetColorMicroService:: processADOData <$eventType> #$id")
		String wiType = getFieldValue('System.WorkItemType', eventType, wiResource)
		if (wiType != 'Bug') return logResult('Not a Bug')
		boolean needColorUpdate
		if (wiResource.fields) {
			needColorUpdate = wiResource.fields.'Microsoft.VSTS.Common.Priority' != null ||
							wiResource.fields.'Microsoft.VSTS.Common.Severity' != null ||
							wiResource.fields.'Custom.Color' != null
		} else {
			needColorUpdate = false
		}
		if (!needColorUpdate) return logResult('No change to Severity, Priority or Color')
		Integer priority = getFieldValue('Microsoft.VSTS.Common.Priority', eventType, wiResource)
		String severity = getFieldValue('Microsoft.VSTS.Common.Severity', eventType, wiResource)
		String color = getFieldValue('Custom.Color', eventType, wiResource)
		String project = getFieldValue('System.TeamProject', eventType, wiResource)
		String rev = getRootFieldValue('rev', eventType, wiResource)
		if (priority != null && severity != 'null') {
			// Get associated color
			String newColor = lookupColor(priority, severity)
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
	private def lookupColor(Integer priority, String severity) {
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
		log.info("Result: $msg")
		return msg
	}

	@Override
	public String topic() {
		return null
	}
	@Override
	public String[] topics() {
		return ['workitem.updated','workitem.created']
	}

}

