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
 * Assigns unassigned tasks to the parent's owner when the task is closed.
 * 
 * @author z097331
 *
 */
@Component
@Slf4j
class SetColorMicroService extends AbstractWebSocketMicroService {
	def colorMap
	@Autowired
	WorkManagementService workManagementService

	@Autowired
	SharedAssetService sharedAssetService

	@Value('${tfs.collection:}')
	String collection	

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
	void loadColorMap() {
		this.colorMap = sharedAssetService.getAsset('colorMap')
	}
	/**
	 * Perform assignment operation
	 * 
	 * @see com.zions.vsts.services.ws.client.AbstractWebSocketMicroService#processADOData(java.lang.Object)
	 */
	@Override
	public Object processADOData(Object adoData) {
		log.info("Entering SetColorMicroService:: processADOData")
		if (!colorMap) loadColorMap()
//		Uncomment code below to capture adoData payload for test
//		String json = new JsonBuilder(adoData).toPrettyString()
//		println(json)
		def outData = adoData
		def wiResource = adoData.resource
		String wiType = "${wiResource.revision.fields.'System.WorkItemType'}"
		if (wiType != 'Bug') return logResult('Not a Bug')
		boolean needColorUpdate = wiResource.fields.'Microsoft.VSTS.Common.Priority' != null ||
								wiResource.fields.'Microsoft.VSTS.Common.Severity' != null ||
								wiResource.fields.'Custom.Color' != null
		if (!needColorUpdate) return logResult('No change to Severity, Priority or Color')
		def priority = wiResource.revision.fields.'Microsoft.VSTS.Common.Priority'
		def severity = "${wiResource.revision.fields.'Microsoft.VSTS.Common.Severity'}"
		def color = "${wiResource.revision.fields.'Custom.Color'}"
		String project = "${wiResource.revision.fields.'System.TeamProject'}"
		String id = "${wiResource.revision.id}"
		String rev = "${wiResource.revision.rev}"
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
	private def lookupColor(def priority, def severity) {
		def colorElement = colorMap.find{it.Priority==priority && it.Severity==severity}
		return "${colorElement.Color}"
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
		log.info(msg)
		return msg
	}

	@Override
	public String topic() {
		return this.eventTopic;
	}

}

