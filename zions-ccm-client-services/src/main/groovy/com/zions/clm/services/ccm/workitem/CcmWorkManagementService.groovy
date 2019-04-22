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
import com.zions.clm.services.ccm.client.CcmGenericRestClient
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
import groovy.xml.XmlUtil
import groovyx.net.http.ContentType

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
	
	@Autowired(required=false)
	CcmGenericRestClient ccmGenericRestClient

	int newId = -1
	
	public CcmWorkManagementService() {
		
	}
	
	def resetNewId() {
		newId = -1
	}
	
	def getWorkitem(id) {
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
	void getWILinkChanges(int id, String project, linkMapping, Closure closure) {
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		IWorkItemClient workItemClient = teamRepository.getClientLibrary(IWorkItemClient.class)
		IWorkItem workItem = workItemClient.findWorkItemById(id, IWorkItem.FULL_PROFILE, null);
		Date modified = workItem.modified()
		String sid = "${id}"
		def cacheWI = cacheManagementService.getFromCache(sid, ICacheManagementService.WI_DATA)
		if (cacheWI != null) {
			def cid = cacheWI.id
			List<LinkInfo> info = this.getAllLinks(sid, modified, workItem, linkMapping)
			def resultLinks = getLinks('affects_execution_result',info)
			resultLinks.each { LinkInfo link ->
				def result = cacheManagementService.getFromCache(link.itemIdRelated, 'QM', ICacheManagementService.RESULT_DATA)
				if (result) {
					def resultChanges = [method:'patch', requestContentType: ContentType.JSON, contentType: ContentType.JSON, uri: "/${eproject}/_apis/test/Runs/${result.testRun.id}/results/${result.id}", query:['api-version':'5.0-preview.5'], body: []]
					def data = [id: result.id, associatedBugs: []]
					def wis = []
					result.associatedBugs.each { bug ->
						String bid = "${bug.id}"
						wis.add(bid)
					}
					String wid = "${cid}"
					if (!wis.contains(wid)) {
						wis.add(wid)
						data.associatedBugs = wis
						resultChanges.body.add(data)
						def changes = [resultChanges: resultChanges, rid: link.itemIdRelated]
						closure.call('Result', changes)
					}
				}
			}
			
			if (resultLinks.size() > 0) {
				cacheWI = cacheManagementService.getFromCache(sid, ICacheManagementService.WI_DATA)
			}
			def wiData = [method:'PATCH', uri: "/_apis/wit/workitems/${cid}?api-version=5.0-preview.3", headers: ['Content-Type': 'application/json-patch+json'], body: []]
			def rev = [ op: 'test', path: '/rev', value: cacheWI.rev]
			wiData.body.add(rev)
			wiData = generateWILinkChanges(wiData, info, linkMapping, cacheWI)
			if (wiData.body.size() == 1) {
				closure.call('WorkItem', wiData)
			}

		}
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
	def generateWILinkChanges(def wiData, List<LinkInfo> links, linkMapping, cacheWI) {
		//def linksList = links.split(',')
		links.each { LinkInfo info -> 
			String id = info.itemIdRelated
			String module = info.moduleRelated
			def url = null
			def linkMap = linkMapping[info.type]
			def runId = null
			def linkId = null
			if (linkMap) {
				if (info.type != 'affects_execution_result') {
					def linkWI = cacheManagementService.getFromCache(id, module, ICacheManagementService.WI_DATA)
					if (linkWI) {
						linkId = linkWI.id
						url = "${tfsUrl}/_apis/wit/workItems/${linkId}"
					}
				}
				if (linkId && !linkExists(cacheWI, linkMap.target, linkId, runId) && "${linkId}" != "${cacheWI.id}") {
					def change = [op: 'add', path: '/relations/-', value: [rel: "${linkMap.@target}", url: url, attributes:[comment: "${linkMap.@source}"]]]
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
	boolean linkExists(cacheWI, targetName, linkId, String runId = null) {
		def url = "${tfsUrl}/_apis/wit/workItems/${linkId}"
		if (runId) {
			url = "${tfsUrl}/_TestManagement/Runs?_a=resultSummary&runId=${runId}&resultId=${linkId}"
		}
		def link = cacheWI.relations.find { rel ->
			"${rel.rel}" == "${targetName}" && url == "${rel.url}"
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
	//@Cache( elementType = WorkitemChanges)
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
	public List<LinkInfo> getAllLinks(String id, Date timeStamp, workItem, linkMapping) {
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
					rid = resolveId(rid, module)
					if ("${linkMap.@key}".length() > 0) {
						rid = "${rid}-${linkMap.@key}"
					}
					//log.info("Related ID for work item (${id}):  ${module} ${rid}")
					if (rid != null) {
						def info = new LinkInfo(type: key, itemIdCurrent: id, itemIdRelated: rid, moduleCurrent: 'CCM', moduleRelated: module)				
						links.add(info)
					}
				}
			}

		}
		return links
	}
	
	String resolveId(String cId, String module) {
		String id = null
		if (cId.startsWith('http')) {
			try {
				String url = cId
				def result = ccmGenericRestClient.get(
				contentType: ContentType.XML,
				uri: cId,
				headers: [Accept: 'application/rdf+xml'] );
				if (!result) {
					return null
				}
				if (module == 'QM') {
					def identifier = result.'**'.find { node ->
				
						node.name() == 'shortId'
					}
//					String xml = new XmlUtil().serialize(result)
//					File outXml = new File('clm.xml')
//					def os = outXml.newDataOutputStream()
//					os << xml
//					os.close()
					id = "${identifier.text()}"
				} else if (module == 'RM') {
					def identifier = result.'**'.find { node ->
				
						node.name() == 'identifier'
					}
//					String xml = new XmlUtil().serialize(result)
//					File outXml = new File('clm.xml')
//					def os = outXml.newDataOutputStream()
//					os << xml
//					os.close()

					id = "${identifier.text()}"
				}
			} catch (e) {}
		} else {
			id = cId
		}
		return id
	}
	
	IProgressMonitor getMonitor() {
		return rtcRepositoryClient.getMonitor()
	}

}


