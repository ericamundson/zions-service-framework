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
class SummaryHandler implements IFieldHandler {
	@Autowired
	RtcRepositoryClient rtcRepositoryClient
	@Autowired
	WorkitemAttributeManager workitemAttributeManager

	def jsonMetaData = null
	
	static int SIZE = 255

	public SummaryHandler() {}


	@Override
	public Object execute(Object data) {
		IWorkItem wi = data.workItem
		def fieldMap = data.fieldMap
		def wiCache = data.cacheWI
		def memberMap = data.memberMap
		String summary = wi.getHTMLSummary().plainText
		int sLength = summary.length()
		String sId = "${wi.id}"
		
		if (sLength > SIZE) {
			sLength = SIZE
			summary = summary.substring(0, SIZE-1)
		}
		def retVal = [op:'add', path:"/fields/${fieldMap.target}", value: summary]
		if (wiCache != null) {
			def cVal = wiCache.fields."${fieldMap.target}"
			if ("${cVal}" == "${retVal.value}") {
				return null
			}
		}
		return retVal;
	}
	
	private String getContributorAsString(Object value) {
		if (value == null) {
			return null;
		}
		if (!(value instanceof IContributorHandle)) {
			throw new Exception(
			"Convert Contributor - Incompatible Type Exception: "
			+ value.toString());
		}
		IContributor contributor = (IContributor) rtcRepositoryClient.getRepo()
				.itemManager().fetchCompleteItem((IContributorHandle) value,
				IItemManager.DEFAULT, rtcRepositoryClient.getMonitor());
		return contributor.name;
	}

	def formatToHTMLOut(IComment[] comments) {
		def writer = new StringWriter()
		MarkupBuilder bHtml = new MarkupBuilder(writer)
		List commentList = []
		comments.each { change ->
			commentList.add(change)
		}
		commentList.reverseEach { IComment comment ->
			bHtml.div(style:'border:2px solid black') {
				div {
					String contributor = getContributorAsString(comment.creator)
					String dateStr = comment.creationDate.format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
					strong("${contributor}:")
					bold("${dateStr}")
				}
				String htmlContent = comment.getHTMLContent()
				div { mkp.yieldUnescaped htmlContent }
			}
		}
		return writer.toString()
	}



}
