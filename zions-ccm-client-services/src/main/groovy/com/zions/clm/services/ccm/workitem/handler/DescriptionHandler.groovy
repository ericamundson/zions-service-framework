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
import com.zions.common.services.attachments.IAttachments
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.work.handler.IFieldHandler
import groovy.json.StringEscapeUtils
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
@Slf4j
class DescriptionHandler implements IFieldHandler {
	@Autowired
	RtcRepositoryClient rtcRepositoryClient
	
	@Autowired
	WorkitemAttributeManager workitemAttributeManager
	
	@Autowired
	ClmWorkItemManagementService clmWorkItemManagementService
	
	@Autowired
	IAttachments attachmentService
	
	@Autowired
	ICacheManagementService cacheManagementService
	
	@Autowired
	@Value('clm.url')
	String clmUrl

	def jsonMetaData = null
	
	static int SIZE = 255

	public DescriptionHandler() {}


	@Override
	public Object execute(Object data) {
		IWorkItem wi = data.workItem
		def fieldMap = data.fieldMap
		def wiCache = data.cacheWI
		def memberMap = data.memberMap
		String description = wi.getHTMLDescription().getXMLText()
		String sId = "${wi.id}"
		description = processImages(description, sId)
		
		def retVal = [op:'add', path:"/fields/${fieldMap.target}", value: description]
		if (wiCache != null) {
			def cVal = wiCache.fields["${fieldMap.target}"]
			if ("${cVal}" == "${retVal.value}") {
				return null
			}
		}
		return retVal;
	}
	
	String processImages(String html, String sId) {
		def htmlData = new XmlSlurper().parseText(html)
		def imgs = htmlData.'**'.findAll { p ->
			String src = p.@src
			"${p.name()}" == 'img' && "${src}".startsWith(this.clmUrl)
		}
		imgs.each { img ->
			String url = img.@src
			def oData = clmWorkItemManagementService.getContent(url)
			def file = cacheManagementService.saveBinaryAsAttachment(oData.data, oData.filename, sId)
			def attData = attachmentService.sendAttachment([file:file])
			img.@src = attData.url
		}
		String outHtml = XmlUtil.asString(htmlData)
		return outHtml 
	}


}
