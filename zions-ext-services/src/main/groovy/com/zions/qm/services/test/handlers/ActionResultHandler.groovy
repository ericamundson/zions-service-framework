package com.zions.qm.services.test.handlers

import com.zions.common.services.attachments.IAttachments
import com.zions.qm.services.test.ClmTestAttachmentManagementService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component('QmActionResultHandler')
class ActionResultHandler extends QmBaseAttributeHandler {
	

	@Autowired
	ClmTestAttachmentManagementService clmAttachmentManagementService
	
	@Autowired
	IAttachments attachments
	
	@Override
	public String getQmFieldName() {
		// TODO Auto-generated method stub
		return 'subResults';
	}

	@Override
	public Object formatValue(def value, def data) {
		// TODO Auto-generated method stub
		def itemData = data.itemData
		def exDataBody = data.exData.body
		def id = data.id
		def adoResult = data.cacheWI
		def webId = "${id}-Result"
		String state = "${itemData.state.text()}"
		def stepResults = itemData.stepResults.stepResult
		String overallOutcome = 'Passed'
		String overallCompletedDate = "${itemData.endtime.text()}"
		String overallStartedDate = "${itemData.starttime.text()}"
		if (stepResults.size()>0) {
			int sid = 2
			def actionResults = []
			def attachmentsList = []
			stepResults.each { stepResult ->
				String result = "${stepResult.@result}"
				String outcome = getOutcome(result)				
				String startedDate = "${stepResult.@startTime}"
				String completedDate = "${stepResult.@endTime}"
				//overallCompletedDate = "${stepResult.@endTime}"
				String sidStr = String.format("%08d", sid)
				def actionResult = [outcome: outcome, startedDate: startedDate, completedDate: completedDate, actionPath: sidStr, iterationId: 1]
				actionResults.add(actionResult)
				attachmentsList = processAttachments(sid, sidStr, stepResult, adoResult, webId, attachmentsList)
				sid++
			}
			
			def model = [[id: 1, actionResults:actionResults, outcome: getOutcome(state), completedDate: overallCompletedDate, startedDate: overallStartedDate]]
			return model
		}
		return null
	}
	
	def processAttachments(int sid, String actionPath, stepResult, adoResult, String rwebId, attachmentsList) {
		def binaries = clmAttachmentManagementService.cacheStepAttachmentsAsBinary(stepResult)
		if (binaries.size() > 0) {
			//def attMap = attachments.ensureResultAttachments(adoResult, binaries, rwebId)
			binaries.each { b ->
				b.comment = "Step ${sid-1} attachment."
				b.actionPath = actionPath
				if (!attachementExists(adoResult, b)) {
					def att = attachments.sendManualResultAttachment(adoResult, b)
				}
				//String fName = b.filename
				//def att = attMap[fName]
				//def attOut = [iterationId: 1, name:fName, size: att.size, id: att.id, url: att.url]
				//attachmentsList.add(attOut)
			}
		}
		return attachmentsList
	}
	
	boolean attachementExists(adoResult, b) {
		if (!adoResult.iterationDetails[0]) return false
		def a = adoResult.iterationDetails[0].attachments.find { attachment ->
			String aName = "${attachment.name}"
			String fName = "${b.filename}"
			aName == fName
		}
		if (a) return true
		return false			
	}
		
	String getOutcome(String state) {
		if (state.endsWith('.passed')) return 'Passed'
		if (state.endsWith('.paused')) return 'Paused'
		if (state.endsWith('.inprogress')) return 'Inprogress'
		if (state.endsWith('.notrun')) return 'NotExecuted'
		if (state.endsWith('.perm_failed')) return 'Failed'
		if (state.endsWith('.incomplete')) return 'Paused'
		if (state.endsWith('.inconclusive')) return 'Inconclusive'
		if (state.endsWith('.part_blocked')) return 'Blocked'
		if (state.endsWith('.deferred')) return 'NotExecuted'
		if (state.endsWith('.failed')) return 'Failed'
		if (state.endsWith('.error')) return 'Failed'
		if (state.endsWith('.blocked')) return 'Blocked'
		return 'Unspecified'
	}
}
