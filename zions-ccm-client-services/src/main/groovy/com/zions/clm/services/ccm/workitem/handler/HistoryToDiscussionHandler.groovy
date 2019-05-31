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
import com.ibm.team.scm.common.IChangeSet
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

@Component('CcmHistoryToDiscussionHandler')
@Slf4j
class HistoryToDiscussionHandler extends CcmBaseAttributeHandler {
	@Autowired
	RtcRepositoryClient rtcRepositoryClient
	@Autowired
	WorkitemAttributeManager workitemAttributeManager
	@Autowired
	CcmWIMetadataManagementService ccmWIMetadataManagementService
	@Autowired
	ClmWorkItemManagementService clmWorkItemManagementService
	
	def jsonMetaData = null
	
	def boolean useValueMap = false
		
	public HistoryHandler() {}


	@Override
	public Object execute(Object data) {
		IWorkItem wi = data.workItem
		def fieldMap = data.fieldMap
		def wiCache = data.cacheWI
		def memberMap = data.memberMap
		def wiMap = data.wiMap
		def history = clmWorkItemManagementService.getWorkItemHistory(wi.id)
		int count = 0
		while (history == null && count < 10) {
			history = clmWorkItemManagementService.getWorkItemHistory(wi.id)
			count++
		}
		if (history == null) {
			return null
		}
		def retVal = null
		if (wiCache != null) {
			String modified = wiCache.fields['System.ChangedDate']
			if (modified != null && modified.length()> 0) {
				if (modified.lastIndexOf('.') > -1) {
					modified = modified.substring(0, modified.lastIndexOf('.'))
				} else {
					modified = modified.replace('Z', '')
				}
				modified = modified + ".999Z"
				Date modDate = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", modified)
				String outHTML = formatToHTMLOut(history, wi, wiMap, modDate)
				if (outHTML == null) return null
				retVal = [op:'add', path:"/fields/${fieldMap.target}", value: outHTML]
			}
		} else {
			String outHTML = formatToHTMLOut(history, wi, wiMap, null)
			if (outHTML == null) return null
			retVal = [op:'add', path:"/fields/${fieldMap.target}", value: outHTML]

		}
		return retVal;
	}
	
	def formatToHTMLOut(history, wi, wiMap, Date modified) {
		def writer = new StringWriter()
		MarkupBuilder bHtml = new MarkupBuilder(writer)
		List changesList = []
		history.'soapenv:Body'.response.returnValue.'value'.changes.each { change ->
			changesList.add(change)
		}
		int count = 0
		changesList.reverseEach { change ->
			Date modifiedDate = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", change.modifiedDate)
			if (modified == null || modifiedDate.time > modified.time) {
				bHtml.div(style:'border:2px solid black') {
					div { 
						strong("${change.modifiedBy.label}:")
						bold("${change.modifiedDate}")
					}
					if (!useValueMap) {
						String htmlContent = change.content
						div {
							mkp.yieldUnescaped htmlContent
						}
					} else {
						String htmlContent = change.content
						htmlContent = translateToVSTS(htmlContent, wi, wiMap)
						div {
							mkp.yieldUnescaped htmlContent
						}
	
					}
				}
				count++
			}
		}
		if (count == 0) return null
		return writer.toString()
	}
	
	def translateToVSTS(htmlContent, wi, wiMap) {
		def witMeta = getWIMetaData(wi)
		def xml = new XmlSlurper().parseText(htmlContent)
	}
	
	def getWIMetaData(IWorkItem wi) {
		if (jsonMetaData == null) {
			IProjectArea pa = ProcessAreaUtil.resolveProjectArea(wi.getProjectArea(), rtcRepositoryClient.getMonitor())
			jsonMetaData = ccmWIMetadataManagementService.extractWorkitemMetadataJson(pa)
		}
		
		String wit = wi.getWorkItemType()
		def metaWit = jsonMetaData["${wit}"]
		return metaWit
	}
	
	def getWIState(IWorkItem wi) {
		if (jsonMetaData == null) {
			IProjectArea pa = ProcessAreaUtil.resolveProjectArea(wi.getProjectArea(), rtcRepositoryClient.getMonitor())
			jsonMetaData = ccmWIMetadataManagementService.extractWorkitemMetadataJson(pa)
		}
		
		String wit = wi.getWorkItemType()
		def metaWit = jsonMetaData["${wit}"]
		if (metaWit == null) {
			log.debug("${wit}: work item type not found!")
			return [:]
		}
		def state = [:]
		metaWit.each { attr ->
			def val = ""
			if ("${attr.id}" == 'internalComments') {
				val = "${wi.getComments().contents.size()}"
			} else {
				val = workitemAttributeManager.getStringRepresentation(wi, wi.getProjectArea(), attr.id)
			}
			if (val == null) val = "<no value>"
			def attrState = [id: attr.id, displayName: attr.displayName, value: val]
			state["${attr.id}"] = attrState
		}
		return state
	}
	
	def deltaVersions(wiState, prevState) {
		def changes = []
		wiState.each { key, state ->
			String fieldId = "${key}"
			if (fieldId.equals('modified') || fieldId.equals('modifiedBy')) return;
			def prev = prevState["${key}"]
			if (prev != null && "${prev.value}" != "${state.value}") {
				def change = [id: state.id, displayName: state.displayName, from: "${prev.value}", to: "${state.value}"]
				if (fieldId.equals('internalComments')) {
					change = [id: state.id, displayName: state.displayName, from: '', to: "new comments added"]
				}
				changes.add(change)
			}
		}
		return changes
	}



}
