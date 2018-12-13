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
import com.ibm.team.workitem.client.IWorkItemClient
import com.ibm.team.workitem.common.model.IAttribute
import com.ibm.team.workitem.common.model.IComment
import com.ibm.team.workitem.common.model.IWorkItem
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
@Slf4j
class MbIdHandler implements IFieldHandler {
	@Autowired
	RtcRepositoryClient rtcRepositoryClient
	@Autowired
	WorkitemAttributeManager workitemAttributeManager

	def jsonMetaData = null
	
	static int SIZE = 255

	public IdHandler() {}


	@Override
	public Object execute(Object data) {
		IWorkItem wi = data.workItem
		def fieldMap = data.fieldMap
		def wiCache = data.cacheWI
		def memberMap = data.memberMap
		String sId = "RTC-${wi.id}"
		String fieldId = "externalid"
		IWorkItemClient workItemClient = rtcRepositoryClient.repo.getClientLibrary(IWorkItemClient.class)
		
		IAttribute attribute = workItemClient.findAttribute(wi.projectArea, fieldId, null);
		String eId = wi.getValue(attribute)
		if (eId != null && eId.length() > 0) {
			sId = sId + " ${eId}"
		}
		
		def retVal = [op:'add', path:"/fields/${fieldMap.target}", value: "${sId}"]
		if (wiCache != null) {
			def cVal = wiCache.fields["${fieldMap.target}"]
			if ("${cVal}" == "${retVal.value}") {
				return null
			}
		}
		return retVal;
	}
	


}
