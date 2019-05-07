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
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.work.handler.IFieldHandler
import groovy.json.StringEscapeUtils
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil
import groovyx.net.http.ContentType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component('CcmCommentsIntoHistoryHandler')
@Slf4j
class CommentsIntoHistoryHandler extends CcmBaseAttributeHandler {
	@Autowired
	RtcRepositoryClient rtcRepositoryClient
	@Autowired
	WorkitemAttributeManager workitemAttributeManager
	
	@Autowired
	IGenericRestClient genericRestClient
	
	def jsonMetaData = null

	public CommentsIntoHistoryHandler() {}


	@Override
	public Object execute(Object data) {
		IWorkItem wi = data.workItem
		def fieldMap = data.fieldMap
		def wiCache = data.cacheWI
		def memberMap = data.memberMap
		IComment[] comments = wi.getComments().getContents()
		def retVal = null
		if (wiCache != null) {
			String modified = wiCache.fields['System.ChangedDate']
			Date modDate = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", modified)
			String outHTML = formatAllToHTMLOut(comments, modDate)
			if (outHTML == null) {
				return null
			}
			retVal = [op:'add', path:"/fields/${fieldMap.target}", value: outHTML]
		} else {
			String outHTML = formatAllToHTMLOut(comments, null)
			if (outHTML == null) {
				return null
			}
			retVal = [op:'add', path:"/fields/${fieldMap.target}", value: outHTML]

		}
			
		return retVal;
	}
	
	def getVSTSHistory(def wiCache) {
		String url = "${wiCache.'_links'.workItemHistory.href}"
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			//requestContentType: ContentType.JSON,
			uri: url,
			headers: ['Content-Type': 'application/json']
			)

		return result;
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

	def formatToHTMLOut(IComment comment) {
		def writer = new StringWriter()
		MarkupBuilder bHtml = new MarkupBuilder(writer)
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
		return writer.toString()
	}

	def formatAllToHTMLOut(IComment[] comments, Date modified) {
		def writer = new StringWriter()
		MarkupBuilder bHtml = new MarkupBuilder(writer)
		List commentList = []
		comments.each { change ->
			commentList.add(change)
		}
		int count = 0
		commentList.reverseEach { IComment comment ->
			Date commentDate = comment.creationDate
			if (modified == null || commentDate.time > modified.time) {
				bHtml.div(style:'border:2px solid black') {
					div {
						String contributor = getContributorAsString(comment.creator)
						String dateStr = commentDate.format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
						strong("${contributor}:")
						bold("${dateStr}")
					}
					String htmlContent = comment.getHTMLContent()
					div { mkp.yieldUnescaped htmlContent }
				}
				count++
			}
		}
		if (count == 0) return null
		return writer.toString()
	}


}
