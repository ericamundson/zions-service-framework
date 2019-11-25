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
import com.ibm.team.workitem.common.model.IResolution
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

@Component('CcmCoreSubStateHandler')
@Slf4j
class CoreSubStateHandler extends CcmBaseAttributeHandler {
	@Autowired
	RtcRepositoryClient rtcRepositoryClient
	@Autowired
	WorkitemAttributeManager workitemAttributeManager

	def jsonMetaData = null
	
	static int SIZE = 255

	public CoreSubStateHandler() {}


	@Override
	public Object execute(Object data) {
		IWorkItem wi = data.workItem
		def fieldMap = data.fieldMap
		def wiCache = data.cacheWI
		def memberMap = data.memberMap
		Identifier<IResolution> resolution = wi.getResolution2()
		IWorkflowInfo wfInfo = getWorkItemCommon().findWorkflowInfo(wi,
				getMonitor());
		String aVal = wfInfo.getResolutionName(resolution);
		if (aVal == null) {
			return null;
		}
		if (aVal.startsWith('Accepted')) {
			aVal = 'Accepted'
		}
		
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
