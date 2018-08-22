package com.zions.clm.services.ccm.workitem

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import com.ibm.team.repository.client.ITeamRepository
import com.ibm.team.workitem.client.IWorkItemClient
import com.ibm.team.workitem.common.model.AttributeTypes
import com.ibm.team.workitem.common.model.IAttribute
import com.ibm.team.workitem.common.model.IWorkItem
import com.zions.clm.services.ccm.client.RtcRepositoryClient
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
	RtcRepositoryClient rtcRepositoryClient
	@Autowired
	WorkitemAttributeManager workitemAttributeManager
	
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
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		IWorkItemClient workItemClient = teamRepository.getClientLibrary(IWorkItemClient.class)
		IWorkItem workItem = workItemClient.findWorkItemById(id, IWorkItem.FULL_PROFILE, null);
		String type = workitemAttributeManager.getStringRepresentation(workItem, workItem.getProjectArea(), 'workItemType')
		def wiMap = translateMapping["${type}"]
		if (wiMap == null) {
			log.error("Map for work item type not found:  ${type}")
			return null
		}
		def outType = "${wiMap.target}"
		return generateWIData(workItem, id, project, outType, wiMap, memberMap)
	}
	
	def getWILinkChanges(id, project, linkMapping) {
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		IWorkItemClient workItemClient = teamRepository.getClientLibrary(IWorkItemClient.class)
		IWorkItem workItem = workItemClient.findWorkItemById(id, IWorkItem.FULL_PROFILE, null);
		def cacheWI = getCacheWI(id)
		if (cacheWI != null) {
			def cid = cacheWI.id
			def wiData = [method:'PATCH', uri: "/${eproject}/_apis/wit/workitems/${cid}?api-version=5.0-preview.3", headers: ['Content-Type': 'application/json-patch+json'], body: []]
			def rev = [ op: 'test', path: '/rev', value: cacheWI.rev]
			wiData.body.add(rev)
			linkMapping.each { key, linkMap ->
				String linkIds = workitemAttributeManager.getStringRepresentation(workItem, workItem.getProjectArea(), "${key}")
				wiData = generateLinkChanges(wiData, linkIds, key, linkMap, cacheWI)
			}
			if (wiData.body.size() == 1) {
				return null
			}
			return wiData
		}
		return null
	}
	
	def generateLinkChanges(def wiData, String linkIds, key, linkMap, cacheWI) {
		def linksList = linkIds.split(',')
		linksList.each { id -> 
			def linkWI = getCacheWI(id)
			if (linkWI != null) {
				def linkId = linkWI.id
				if (!linkExists(cacheWI, linkMap.target, linkId)) {
					def change = [op: 'add', path: '/relations/-', value: [rel: "${linkMap.target}", url: "${tfsUrl}/_apis/wit/workItems/${linkId}", attributes:[comment: "${link.source} of ${linkId}"]]]
					wiData.body.add(change)
				}
			}
		}
		return wiData
	}
	
	boolean linkExists(cacheWI, targetName, linkId) {
		def link = cacheWI.relations.find { rel ->
			"${rel.rel}" == "${targetName}" && "${tfsUrl}/_apis/wit/workItems/${linkId}" == "${rel.url}"
		}
		return link != null
	}

	def generateWIData(workItem, id, project, type, wiMap, memberMap) {
		def etype = URLEncoder.encode(type, 'utf-8').replace('+', '%20')
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def wiData = [method:'PATCH', uri: "/${eproject}/_apis/wit/workitems/\$${etype}?api-version=5.0-preview.3&bypassRules=true", headers: ['Content-Type': 'application/json-patch+json'], body: []]
		def cacheWI = getCacheWI(id)
		if (cacheWI != null) {
			def cid = cacheWI.id
			wiData = [method:'PATCH', uri: "/${eproject}/_apis/wit/workitems/${cid}?api-version=5.0-preview.3", headers: ['Content-Type': 'application/json-patch+json'], body: []]
			def rev = [ op: 'test', path: '/rev', value: cacheWI.rev]
			wiData.body.add(rev)
		} else {
			def idData = [ op: 'add', path: '/id', value: newId]
			newId--
			wiData.body.add(idData)
		}
		wiMap.fieldMaps.each { fieldMap -> 
			def fieldData = getFieldData(workItem, fieldMap, cacheWI, memberMap)
			if (fieldData != null) {
				wiData.body.add(fieldData)
			}
			
		}
		return wiData
	}
		
	def getFieldData(IWorkItem workItem, def fieldMap, def cacheWI, memberMap) {
		String attributId = "${fieldMap.source}"
		String fValue = ""
		if (attributId.trim().equals("")) {
			fValue = "any"
		} else {
			fValue = workitemAttributeManager.getStringRepresentation(workItem, workItem.getProjectArea(), attributId)
			if (fValue == null) return null
		}
		
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		IWorkItemClient workItemClient = teamRepository.getClientLibrary(IWorkItemClient.class)		
		IAttribute attribute = workItemClient.findAttribute(workItem.getProjectArea(), attributId, null);
		String attribType = attribute.getAttributeType()
		if (attribType.equals(AttributeTypes.CONTRIBUTOR) && memberMap[fValue.toLowerCase()] == null) {
			return null
		} else if (attribType.equals(AttributeTypes.CONTRIBUTOR) && memberMap[fValue.toLowerCase()] != null) {
			fValue = "${memberMap[fValue.toLowerCase()].uniqueName}"
		}
		String cValue = ""
		if (cacheWI != null) {
			cValue = "${cacheWI.fields["${fieldMap.target}"]}"
			if ("${fValue}" != "${cValue}") {
				def val = "${fValue}"
				if (fieldMap.valueMap.size() > 0) {
					
					fieldMap.valueMap.each { aval ->
						if ("${val}" == "${aval.source}") {
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
				return [op:'replace', path: "/fields/${fieldMap.target}", value: val]
			} else {
				return null
			}
		}
		def val = "${fValue}"
		if (fieldMap.valueMap.size() > 0) {
			
			fieldMap.valueMap.each { aval ->
				if ("${val}" == "${aval.source}") {
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
	
	def getCacheWI(id) {
		File cacheData = new File("${this.cacheLocation}${File.separator}${id}${File.separator}wiData.json");
		if (cacheData.exists()) {
			JsonSlurper s = new JsonSlurper()
			return s.parse(cacheData)
		}
		return null

	}
}
