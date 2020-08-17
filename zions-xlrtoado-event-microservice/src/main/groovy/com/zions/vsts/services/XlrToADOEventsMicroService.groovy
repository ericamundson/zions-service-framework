package com.zions.vsts.services

import com.zions.vsts.services.rmq.mixins.MessageReceiverTrait
import com.zions.vsts.services.build.BuildManagementService
import com.zions.vsts.services.extdata.ExtensionDataManagementService
import com.zions.xlr.services.query.ReleaseQueryService
import com.zions.xlr.services.events.db.XlrEventRepository
import com.zions.xlr.services.events.db.XlrEvent
import com.zions.xlr.services.rest.client.XlrGenericRestClient
//import com.zions.vsts.services.ws.client.WebSocketMicroServiceTrait
import com.zions.xlr.services.events.db.XlrEventRepository
import com.zions.xlr.services.events.db.XlrReleaseSubscription
import com.zions.xlr.services.events.db.XlrReleaseSubscriptionRepository

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Handles sending XLR Release events to an ADO Build.
 * 
 * @author z091182
 *
 */
@Component
@Slf4j
class XlrToADOEventsMicroService implements MessageReceiverTrait {

	@Autowired
	BuildManagementService buildManagementService
	@Autowired
	XlrGenericRestClient xlrGenericRestClient

	@Autowired
	ReleaseQueryService releaseQueryService
	
	@Autowired
	ExtensionDataManagementService extensionDataManagementService
	
	@Autowired
	XlrEventRepository xlrEventRepository
	
	@Autowired
	XlrReleaseSubscriptionRepository xlrReleaseSubscriptionRepository

	public XlrToADOEventsMicroService() {
		
	}

	/**
	 * Perform sending of XLR events to ADO build.
	 * 
	 * @see com.zions.vsts.services.ws.client.AbstractWebSocketMicroService#processADOData(java.lang.Object)
	 */
	public def processADOData(def xlrData) {
		log.info("Entering XlrToADOEventsMicroService:: processADOData")
		XlrReleaseSubscription releaseSub = null
		try {
			releaseSub = xlrReleaseSubscriptionRepository.findByReleaseId(xlrData.releaseId)
		} catch (e) { e.printStackTrace()}
		if (!releaseSub) return
		def release = releaseQueryService.getRelease(xlrData.releaseId)
		String extKey = "XLR_${releaseSub.adoProject}_${releaseSub.pipelineId}"
		if (releaseSub.isReleasePipeline) {
			extKey = "${extKey}_release"
		}
		XlrEvent e = new XlrEvent(xlrData)
		xlrEventRepository.save(e)
		String title = "${release.title}"
		String ruuid = getReleaseUIID(xlrData.releaseId)
		String url = "${xlrGenericRestClient.xlrUrl}/#/releases/${ruuid}"
		def extData = extensionDataManagementService.getExtensionData(extKey)
		if (!extData) {
			extData = [id: extKey, buildId: releaseSub.pipelineId, adoProject: releaseSub.adoProject, releaseName: title, releaseUrl: url, events: []]
		} else {
			extData.releaseName = title
			extData.releaseUrl = url
		}
		List<XlrEvent> events = xlrEventRepository.findByReleaseId(xlrData.releaseId)
		extData.events = events
		extensionDataManagementService.ensureExtensionData(extData)
		return null;
	}
	
	String getReleaseUIID(String releaseId) {
		java.util.regex.Matcher m = releaseId =~ /(Folder)\S*$/
		m.find()
		if (m.size() > 0) {
			
			def a = m[0..-1]
			String out = a[0][0]
			out = out.replace('/', '-')
			return out
		}
		return null
	}

	public String topic() {
		return 'none';
	}

}

