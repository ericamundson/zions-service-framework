package com.zions.clm.services.ccm.workitem.handler

import com.ibm.team.links.client.ILinkManager
import com.ibm.team.links.common.ILink
import com.ibm.team.links.common.ILinkCollection
import com.ibm.team.links.common.ILinkQueryPage
import com.ibm.team.links.common.IReference
import com.ibm.team.links.common.factory.IReferenceFactory
import com.ibm.team.process.common.IProjectArea
import com.ibm.team.repository.client.IItemManager
import com.ibm.team.repository.common.IAuditable
import com.ibm.team.repository.common.IAuditableHandle
import com.ibm.team.repository.common.IContributor
import com.ibm.team.repository.common.IContributorHandle
import com.ibm.team.repository.common.TeamRepositoryException
import com.ibm.team.scm.common.IChangeSet
import com.ibm.team.workitem.common.model.IApproval
import com.ibm.team.workitem.common.model.IApprovalDescriptor
import com.ibm.team.workitem.common.model.IApprovalState
import com.ibm.team.workitem.common.model.IApprovals
import com.ibm.team.workitem.common.model.IComment
import com.ibm.team.workitem.common.model.IWorkItem
import com.ibm.team.workitem.common.model.WorkItemApprovals
import com.zions.clm.services.ccm.client.RtcRepositoryClient
import com.zions.clm.services.ccm.utils.ProcessAreaUtil
import com.zions.clm.services.ccm.workitem.WorkitemAttributeManager
import com.zions.clm.services.ccm.workitem.metadata.CcmWIMetadataManagementService
import com.zions.clm.services.rtc.project.workitems.ClmWorkItemManagementService
import com.zions.common.services.work.handler.IWorkitemFieldHandler
import groovy.json.JsonBuilder
//import groovy.json.StringEscapeUtils
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil

import java.util.Collection

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.apache.commons.lang.StringEscapeUtils;

@Component
@Slf4j
class ApprovalsHandler implements IWorkitemFieldHandler {
	@Autowired
	RtcRepositoryClient rtcRepositoryClient
	@Autowired
	WorkitemAttributeManager workitemAttributeManager

	def jsonMetaData = null

	public ApprovalsHandler() {}


	@Override
	public Object execute(Object data) {
		IWorkItem wi = data.workItem
		def fieldMap = data.fieldMap
		def wiCache = data.cacheWI
		def memberMap = data.memberMap
		IApprovals approvals = wi.getApprovals();
		def retInfo = [approvalStates:[]]
		Map<IApprovalDescriptor, Collection<IApproval>> approvalmap = WorkItemApprovals
				.groupByApprovalDescriptors(approvals);
		List<IApprovalDescriptor> descriptors = approvals.getDescriptors();
		descriptors.each { IApprovalDescriptor desc -> 
			IApprovalState approvalOverAllState = WorkItemApprovals
				.getState(desc.getCumulativeStateIdentifier());
			String stateStr = approvalOverAllState.getDisplayName();
			String approvalType = WorkItemApprovals.getType(desc.typeIdentifier).getDisplayName()
			Date date = desc.getDueDate();
			def approvalState = [name: desc.name, approvalType: approvalType, approvalState: stateStr, approvers: []]
			if (date != null) {
				approvalState.dueDate = date.format('EEE MMM dd YYYY')
			}
			Collection<IApproval> approvers = approvalmap[desc]
			approvers.each { IApproval approver ->
				IContributor contributor = (IContributor) rtcRepositoryClient.getRepo()
						.itemManager().fetchCompleteItem((IContributorHandle) approver.approver,
								IItemManager.DEFAULT, rtcRepositoryClient.getMonitor());
				String approvalStateStr = WorkItemApprovals.getState(approver.getStateIdentifier()).displayName
				def appInfo = [parentName: desc.name, member: contributor.getEmailAddress(), approvalState: approvalStateStr]
				approvalState.approvers.add(appInfo)
			}
			retInfo.approvalStates.add(approvalState)
		}
		def jStr = new JsonBuilder(retInfo).toString()
		String htmlEscape = StringEscapeUtils.escapeHtml(jStr)
		def retVal = [op:'add', path:"/fields/${fieldMap.target}", value: htmlEscape]
		if (wiCache != null) {
			def cVal = wiCache.fields."${fieldMap.target}"
			if ("${cVal}" == "${retVal.value}") {
				return null
			}
		}
		return retVal;

	}
	
	


}
