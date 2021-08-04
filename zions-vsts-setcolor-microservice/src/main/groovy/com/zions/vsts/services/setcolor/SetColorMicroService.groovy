package com.zions.vsts.services.setcolor

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
class SetColorMicroService implements MessageReceiverTrait {
	@Autowired
	WorkManagementService workManagementService

	@Autowired
	SharedAssetService sharedAssetService

	@Value('${tfs.collection:}')
	String collection	

	@Value('${tfs.colorMapUID:}')
	String colorMapUID	

	@Autowired
	public SetColorMicroService() {
	}
	// Handle HTTP 412 retry when work item revision has changed
	def attemptedProject
	def attemptedId
	Closure responseHandler = { resp ->
		// Get fresh copy of work item
		def wi = workManagementService.getWorkItem(collection, attemptedProject, attemptedId)
		Integer priority = wi.fields.'Microsoft.VSTS.Common.Priority'
		String severity = wi.fields.'Microsoft.VSTS.Common.Severity'
		String color = wi.fields.'Custom.Color'
		String rev = "${wi.rev}"
		if (priority != null && severity != 'null') {
			// Get associated color
			String newColor = lookupColor(priority, severity)
			if (color == 'null' || newColor != color) {
				log.info("Second attempt to update color for $attemptedId")
				def resp2 = updateColor(attemptedProject, attemptedId, rev, newColor)
				if (resp2) 
					return resp2
				else {
					log.error('Failed update after 412 retry')
					return null
				}
			}
			else // No update needed, so return true for success
				return true
		}
		else if (color != 'null'){
			// Need to set color to unassigned
			def resp2 = updateColor(attemptedProject, attemptedId, rev, '')
			if (resp2) 
				return resp2
			else {
				log.error('Failed update after 412 retry')
				return null
			}
		}
		
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
		String id = getRootFieldValue('id', wiResource)
		log.debug("Entering SetColorMicroService:: processADOData <$eventType> #$id")
		String wiType = getFieldValue('System.WorkItemType', wiResource)
		if (wiType != 'Bug') return logResult('Not a Bug')
		Integer priority = getFieldValue('Microsoft.VSTS.Common.Priority', wiResource)
		String severity = getFieldValue('Microsoft.VSTS.Common.Severity', wiResource)
		String color = getFieldValue('Custom.Color', wiResource)
		String project = getFieldValue('System.TeamProject', wiResource)
		String rev = getRootFieldValue('rev', wiResource)
		if (priority != null && severity != 'null') {
			// Get associated color
			try {
				String newColor = lookupColor(priority, severity)
				if (color == 'null' || newColor != color) {
					if (updateColor(project, id, rev, newColor, this.responseHandler))
						return logResult("Color updated for Bug #$id")
					else {
						log.error("Error updating color for Bug #$id")
						return 'Failed update'
					}
				}
				else return logResult("No updates needed for Bug #$id")
			}
			catch(Exception e) {
				log.error("Could not retrieve color map information for Bug #$id: ${e.message}")
			}
		}
		else if (color != 'null'){
			// Need to set color to unassigned
			if (updateColor(project, id, rev, ''))
				return logResult("Color set to unassigned for Bug #$id")
			else {
				log.error("Error updating color")
				return 'Failed update'
			}
		}
	}
	
	private def getFieldValue(def field, def wiResource) {
		def value = wiResource.revision.fields[field]

		if (field == 'Microsoft.VSTS.Common.Priority') {
			if (value == null) return null
			return "$value".toInteger()
		} else {
			return "$value"
		}
	}
	private def getRootFieldValue(def field, def wiResource) {
		def value = wiResource.revision[field]
	}
	private def lookupColor(Integer priority, String severity) {
		def colorMap = sharedAssetService.getAsset(collection, colorMapUID)
		def colorElement = colorMap.find{it.Priority==priority && it.Severity==severity}
		return colorElement.Color
	}
	private def updateColor(def project, def id, String rev, String color, Closure handler = null) {
		def data = []
		def t = [op: 'test', path: '/rev', value: rev.toInteger()]
		data.add(t)
		def e = [op: 'add', path: '/fields/Custom.Color', value: color]
		data.add(e)
		this.attemptedProject = project
		this.attemptedId = id
		return workManagementService.updateWorkItem(collection, project, id, data, handler)
	}
	
	private def logResult(def msg) {
		log.info("Result: $msg")
		return msg
	}
}

