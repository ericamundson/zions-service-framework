package com.zions.clm.services.ccm.workitem

import java.util.Map

import org.eclipse.core.runtime.IProgressMonitor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import com.ibm.team.links.common.IReference
import com.ibm.team.links.common.registry.IEndPointDescriptor
import com.ibm.team.repository.client.ITeamRepository
import com.ibm.team.workitem.client.IWorkItemClient
import com.ibm.team.workitem.common.IWorkItemCommon
import com.ibm.team.workitem.common.model.AttributeTypes
import com.ibm.team.workitem.common.model.IAttribute
import com.ibm.team.workitem.common.model.IWorkItem
import com.ibm.team.workitem.common.model.IWorkItemReferences
import com.zions.clm.services.ccm.client.RtcRepositoryClient
import com.zions.clm.services.ccm.utils.ReferenceUtil
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cacheaspect.Cache
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.link.LinkInfo
import com.zions.common.services.work.handler.IFieldHandler
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

/**
 * Provides behavior to process RTC work items in a form to be used to translate to VSTS work items.
 * 
 * @author z091182
 *
 */
@Component
@Slf4j
class CcmWorkManagementService {
	
	@Autowired
	private Map<String, IFieldHandler> fieldMap;
	
	@Autowired
	RtcRepositoryClient rtcRepositoryClient
	@Autowired
	WorkitemAttributeManager workitemAttributeManager
	
	@Autowired
	ICacheManagementService cacheManagementService

	@Autowired
	@Value('${cache.location}')
	String cacheLocation
	
	@Autowired
	@Value('${tfs.url}')
	String tfsUrl

	int newId = -1
	
	public CcmWorkManagementService() {
		
	}
	
	def resetNewId() {
		newId = -1
	}
	
	IWorkItem getWorkitem(id) {
		if (id instanceof String) {
			id = Integer.parseInt(id)
		}
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		IWorkItemClient workItemClient = teamRepository.getClientLibrary(IWorkItemClient.class)
		IWorkItem workItem = workItemClient.findWorkItemById(id, IWorkItem.FULL_PROFILE, null);
		return workItem
	}
	
	/**
	 * Get the data structure of field changes to create/update VSTS work item.
	 * 
	 * @param id
	 * @param project
	 * @param translateMapping
	 * @param memberMap
	 * @return
	 */
	def getWIChanges(id, project, translateMapping, memberMap) {
		IWorkItem workItem = getWorkitem(id)
		String type = workitemAttributeManager.getStringRepresentation(workItem, workItem.getProjectArea(), 'workItemType')
		def wiMap = translateMapping["${type}"]
		if (wiMap == null) {
			log.error("Map for work item type not found:  ${type}")
			return null
		}
		def outType = "${wiMap.target}"
		return generateWIData(id, workItem.modified(), workItem,  project, outType, wiMap, memberMap).changes
	}
	
	/**
	 * Returns link changes from RTC that is in object usable for VSTS work item update request.
	 * 
	 * @param id
	 * @param project
	 * @param linkMapping
	 * @return
	 */
	def getWILinkChanges(String id, String project, linkMapping) {
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		IWorkItemClient workItemClient = teamRepository.getClientLibrary(IWorkItemClient.class)
		IWorkItem workItem = workItemClient.findWorkItemById(id, IWorkItem.FULL_PROFILE, null);
		Date modified = workItem.modified()
		def cacheWI = cacheManagementService.getFromCache(id, ICacheManagementService.WI_DATA)
		if (cacheWI != null) {
			def cid = cacheWI.id
			def wiData = [method:'PATCH', uri: "/_apis/wit/workitems/${cid}?api-version=5.0-preview.3", headers: ['Content-Type': 'application/json-patch+json'], body: []]
			def rev = [ op: 'test', path: '/rev', value: cacheWI.rev]
			wiData.body.add(rev)
			List<LinkInfo> info = this.getAllLinks(id, modified, workItem, linkMapping)
			wiData = generateLinkChanges(wiData, info, linkMapping, cacheWI)
			if (wiData.body.size() == 1) {
				return null
			}
			return wiData
		}
		return null
	}
	
	List<LinkInfo> getLinks(String type, links) {
		List<LinkInfo> out = links.findAll { LinkInfo link ->
			link.type == type
		}
		return out
	}
	
	/**
	 * Generates work item link changes.
	 * 
	 * @param wiData
	 * @param linkIds
	 * @param key
	 * @param linkMap
	 * @param cacheWI
	 * @return
	 */
	def generateLinkChanges(def wiData, List<LinkInfo> links, linkMapping, cacheWI) {
		def linksList = links.split(',')
		links.each { LinkInfo info -> 
			String id = info.itemIdRelated
			String module = info.moduleRelated()
			def linkWI = cacheManagementService.getFromCache(id, module, ICacheManagementService.WI_DATA)
			def linkMap = linkMapping[info.type]
			if (linkWI != null && linkMap) {
				def linkId = linkWI.id
				if (!linkExists(cacheWI, linkMap.target, linkId) && "${linkId}" != "${cacheWI.id}") {
					def change = [op: 'add', path: '/relations/-', value: [rel: "${linkMap.@target}", url: "${tfsUrl}/_apis/wit/workItems/${linkId}", attributes:[comment: "${linkMap.@source}"]]]
					wiData.body.add(change)
				}
			}
		}
		return wiData
	}
	
	
	/**
	 * Check work item cache to see if link exists on work item.
	 * 
	 * @param cacheWI
	 * @param targetName
	 * @param linkId
	 * @return
	 */
	boolean linkExists(cacheWI, targetName, linkId) {
		def link = cacheWI.relations.find { rel ->
			"${rel.rel}" == "${targetName}" && "${tfsUrl}/_apis/wit/workItems/${linkId}" == "${rel.url}"
		}
		return link != null
	}

	/**
	 * Generate work item VSTS change request from RTC datat.
	 * 
	 * @param workItem
	 * @param id
	 * @param project
	 * @param type
	 * @param wiMap
	 * @param memberMap
	 * @return
	 */
	@Cache( elementType = WorkitemChanges)
	WorkitemChanges generateWIData(id, Date timeStamp, workItem,  project, type, wiMap, memberMap) {
		def etype = URLEncoder.encode(type, 'utf-8').replace('+', '%20')
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def wiData = [method:'PATCH', uri: "/${eproject}/_apis/wit/workitems/\$${etype}?api-version=5.0-preview.3&bypassRules=true", headers: ['Content-Type': 'application/json-patch+json'], body: []]
		def cacheWI = cacheManagementService.getFromCache(id, ICacheManagementService.WI_DATA)
		if (cacheWI != null) {
			def cid = cacheWI.id
			wiData = [method:'PATCH', uri: "/_apis/wit/workitems/${cid}?api-version=5.0-preview.3&bypassRules=true", headers: ['Content-Type': 'application/json-patch+json'], body: []]
			def rev = [ op: 'test', path: '/rev', value: cacheWI.rev]
			wiData.body.add(rev)
		} else {
			def idData = [ op: 'add', path: '/id', value: newId]
			newId--
			wiData.body.add(idData)
		}
		wiMap.fieldMaps.each { fieldMap -> 
			def fieldData = getFieldData(workItem, fieldMap, cacheWI, memberMap, wiMap)
			if (fieldData != null) {
				if (!(fieldData instanceof ArrayList)) {
					wiData.body.add(fieldData)
				} else {
					fieldData.each { fData ->
						if (fData.value != null) {
							wiData.body.add(fData)
						}
					}
				}
			}
			
		}
		if (wiData.body.size() == 1) {
			return null
		}
		//String json = new JsonBuilder(wiData).toPrettyString()
		WorkitemChanges data = new WorkitemChanges(changes: wiData)
		return data
	}
		
	/**
	 * Get field specific change data.
	 * 
	 * @param workItem
	 * @param fieldMap
	 * @param cacheWI
	 * @param memberMap
	 * @return
	 */
	def getFieldData(IWorkItem workItem, def fieldMap, def cacheWI, memberMap, wiMap) {
		String attributId = "${fieldMap.source}"
		String fValue = ""
		if (attributId.trim().equals("")) {
			fValue = "any"
		} else {
			fValue = workitemAttributeManager.getStringRepresentation(workItem, workItem.getProjectArea(), attributId)
			if (fValue == null && this.fieldMap["${attributId}"] != null) {
				def data = [workItem: workItem, memberMap: memberMap, fieldMap: fieldMap, cacheWI: cacheWI, wiMap: wiMap]
				return this.fieldMap["${attributId}"].execute(data)
			}
			if (fValue == null) return null
		}
		
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		IWorkItemClient workItemClient = teamRepository.getClientLibrary(IWorkItemClient.class)		
		IAttribute attribute = workItemClient.findAttribute(workItem.getProjectArea(), attributId, null);
		if (attribute != null) {
			String attribType = attribute.getAttributeType()
			if (attribType.equals(AttributeTypes.CONTRIBUTOR) && memberMap[fValue.toLowerCase()] == null) {
				return null
			} else if (attribType.equals(AttributeTypes.CONTRIBUTOR) && memberMap[fValue.toLowerCase()] != null) {
				fValue = "${memberMap[fValue.toLowerCase()].uniqueName}"
			}
		}
		String cValue = ""
		if (cacheWI != null) {
			cValue = "${cacheWI.fields["${fieldMap.target}"]}"
			def val = "${fValue}"
			if (fieldMap.defaultMap != null) {
				val = fieldMap.defaultMap.target
			}
			if (fieldMap.valueMap.size() > 0) {
				
				fieldMap.valueMap.each { aval ->
					if ("${fValue}" == "${aval.source}") {
						val = "${aval.target}"
						return
					}
				}
			}
			if ("${fieldMap.outType}" == 'integer') {
				val = Integer.parseInt(val)
			} else if ("${fieldMap.outType}" == 'double') {
				val = Double.parseDouble(val)
			} else if ("${fieldMap.outType}" == 'boolean') {
				val = Boolean.parseBoolean(val)
			}
			if ("${val}" != "${cValue}") {
				return [op:'add', path: "/fields/${fieldMap.target}", value: val]
			} else {
				return null
			}
		}
		def val = "${fValue}"
		if (fieldMap.defaultMap != null) {
			val = fieldMap.defaultMap.target
		}
		if (fieldMap.valueMap.size() > 0) {
			
			fieldMap.valueMap.each { aval ->
				if ("${fValue}" == "${aval.source}") {
					val = "${aval.target}"
					return
				}
			}
		}
		if ("${fieldMap.outType}" == 'integer') {
			val = Integer.parseInt(val)
		} else if ("${fieldMap.outType}" == 'double') {
			val = Double.parseDouble(val)
		} else if ("${fieldMap.outType}" == 'boolean') {
			val = Boolean.parseBoolean(val)
		}
		return [op:'add', path:"/fields/${fieldMap.target}", value: val]
		
	}
	
	IWorkItemCommon getWorkItemCommon() {
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		return teamRepository.getClientLibrary(IWorkItemCommon.class)
	}


	@Cache(elementType = LinkInfo)
	public List<LinkInfo> getAllLinks(String id, Date timeStamp, IWorkItem workItem, linkMapping) {
		List<LinkInfo> links = new ArrayList<LinkInfo>()
		linkMapping.each { key, linkMap ->
			String linkType = ReferenceUtil.getReferenceType(key);
			String module = 'CCM'
			if ("${linkMap.@module}".length() > 0) {
				module = "${linkMap.@module}"
			}
			String ids = workitemAttributeManager.getStringRepresentation(workItem, workItem.getProjectArea(), "${key}")
			String[] idList = ids.split(',')
			idList.each { String rid ->
				if (rid && rid.length() > 0) {
					def info = new LinkInfo(type: key, itemIdCurrent: id, itemIdRelated: rid, moduleCurrent: 'CCM', moduleRelated: module)				
					links.add(info)
				}
			}

		}
		return links
	}
	
	IProgressMonitor getMonitor() {
		return rtcRepositoryClient.getMonitor()
	}

}


