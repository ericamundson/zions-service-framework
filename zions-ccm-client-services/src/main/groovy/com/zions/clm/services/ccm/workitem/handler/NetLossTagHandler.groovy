package com.zions.clm.services.ccm.workitem.handler

import com.ibm.team.links.client.ILinkManager
import com.ibm.team.links.common.ILink
import com.ibm.team.links.common.ILinkCollection
import com.ibm.team.links.common.ILinkQueryPage
import com.ibm.team.links.common.IReference
import com.ibm.team.links.common.factory.IReferenceFactory
import com.ibm.team.process.common.IProjectArea
import com.ibm.team.repository.client.IItemManager
import com.ibm.team.repository.client.ITeamRepository
import com.ibm.team.repository.common.IAuditable
import com.ibm.team.repository.common.IAuditableHandle
import com.ibm.team.repository.common.IContributor
import com.ibm.team.repository.common.IContributorHandle
import com.ibm.team.repository.common.TeamRepositoryException
import com.ibm.team.scm.common.IChangeSet
import com.ibm.team.workitem.api.common.IState
import com.ibm.team.workitem.common.IWorkItemCommon
import com.ibm.team.workitem.common.model.IComment
import com.ibm.team.workitem.common.model.IWorkItem
import com.ibm.team.workitem.common.model.Identifier
import com.ibm.team.workitem.common.workflow.IWorkflowInfo
import com.zions.clm.services.ccm.client.RtcRepositoryClient
import com.zions.clm.services.ccm.utils.ProcessAreaUtil
import com.zions.clm.services.ccm.workitem.WorkitemAttributeManager
import com.zions.clm.services.ccm.workitem.metadata.CcmWIMetadataManagementService
import com.zions.clm.services.rtc.project.workitems.ClmWorkItemManagementService
import com.zions.common.services.work.handler.IFieldHandler
import groovy.json.StringEscapeUtils
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil

import org.eclipse.core.runtime.IProgressMonitor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
@Slf4j
class NetLossTagHandler implements IFieldHandler {
	@Autowired
	RtcRepositoryClient rtcRepositoryClient
	@Autowired
	WorkitemAttributeManager workitemAttributeManager

	def jsonMetaData = null
	
	static int SIZE = 255

	public MbSubStateHandler() {}


	@Override
	public Object execute(Object data) {
		IWorkItem wi = data.workItem
		def fieldMap = data.fieldMap
		def wiCache = data.cacheWI
		def memberMap = data.memberMap
		Identifier<IState> state = wi.getState2();
		IWorkflowInfo wfInfo = getWorkItemCommon().findWorkflowInfo(wi,
				getMonitor());
		String stateName = wfInfo.getStateName(state);
		String tags = workitemAttributeManager.getStringRepresentation(wi, wi.projectArea, 'internalTags')
		String foundIn = workitemAttributeManager.getStringRepresentation(wi, wi.projectArea, 'foundIn')
		String aVal = null
		if (foundIn == null || foundIn == 'Unassigned') {
			foundIn = '';
		} else {
			foundIn = foundIn.replace(' ', '')
		}
		if ((tags == null || tags.length()==0) && foundIn.length()>0) {
			aVal = "${foundIn}"
		} else if (tags && tags.length()>0 && foundIn.length()>0){
			aVal = "${tags},${foundIn}"
		} else if (tags && tags.length()>0 && foundIn.length()==0) {
			aVal = "${tags}"
		}
		if (aVal == null) return null
		def retVal = [op:'add', path:"/fields/${fieldMap.target}", value: aVal]
		if (wiCache != null) {
			def cVal = wiCache.fields["${fieldMap.target}"]
			if ("${cVal}" == "${retVal.value}") {
				return null
			}
		}
		return retVal;
	}
	
	IWorkItemCommon getWorkItemCommon() {
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		return teamRepository.getClientLibrary(IWorkItemCommon.class)
	}

	IProgressMonitor getMonitor() {
		return rtcRepositoryClient.getMonitor()
	}

}
