package com.zions.jama.services.requirements

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component;

import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.restart.ICheckpointManagementService
import com.zions.common.services.util.ObjectUtil
import com.zions.common.services.work.handler.IFieldHandler
import com.zions.rm.services.requirements.RequirementsMappingManagementService
import com.zions.jama.services.requirements.handlers.RmBaseAttributeHandler
import com.zions.common.services.link.LinkInfo

import groovy.json.JsonSlurper
import groovy.xml.XmlUtil
import groovyx.net.http.ContentType


/**
 * Handles all of the behavior to pull RM data for setting up output data to be sent to ADO to create/update work items.
 * 
 * Design<p/>
 * <img src="ClmRequirementsItemManagementService.png"/>
 * 
 * @author z091182
 * 
 * 
 * @startuml
 * 
 * annotation Component
 * annotation Autowired
 * 
 * class Map<String, IFieldHandler> {
 * }
 * 
 * class ClmRequirementsItemManagementService {
 * .... Idea on entry point method to implement ...
 * +def generateItemData(def rmItemData, def map, String project, def memberMap, def parent = null)
 * }
 * note left: @Component
 * 
 * ClmRequirementsItemManagementService .. Component: Spring component
 * ClmRequirementsItemManagementService .. Autowired: Spring injects instance
 * ClmRequirementsItemManagementService o--> Map: @Autowired fieldMap
 * 
 * 
 * @enduml
 *
 */
@Component
class JamaRequirementsItemManagementService {	
	@Autowired
	@Value('${tfs.url}')
	String tfsUrl

	@Autowired(required=false)
	Map<String, RmBaseAttributeHandler> fieldMap;
	
	@Autowired
	JamaMappingManagementService rmMappingManagementService
	
	@Autowired(required=true)
	ICacheManagementService cacheManagementService

	@Autowired(required=false)
	ICheckpointManagementService checkpointManagementService

	int newId = -1
	
	def jamaReptionshipTypeMap = [9361:'Related to',9362:'Dependent on',9363:'Derived from']
	
	def resetNewId() {
		newId = -1
	}
	public JamaRequirementsItemManagementService() {
		// Do nothing
	}
	
	def getChanges(String project, def artifact, def memberMap, def runData = null) {
		String type = artifact.getArtifactType()
		def maps = getReqMaps(type)
		def outItems = [:]
		maps.each { map ->
			def item = generateItemData(artifact, map, project, memberMap)
			if (item != null) {
				outItems["${map.target}"] = item
			}
		}
		return outItems
	}
	
	def generateItemData(def rmItemData, def map, String project, def memberMap, def parent = null) {
		// Save TFS workitem type into the rm artifact
		rmItemData.setTfsWorkitemType("${map.target}")
		
		
		def etype = URLEncoder.encode("${map.target}", 'utf-8').replace('+', '%20')
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def wiData = [method:'PATCH', uri: "/${eproject}/_apis/wit/workitems/\$${etype}?api-version=5.0-preview.3&bypassRules=true", headers: ['Content-Type': 'application/json-patch+json'], body: []]
		String id = "${rmItemData.getCacheID()}"
		def cacheWI = cacheManagementService.getFromCache(id, ICacheManagementService.WI_DATA)
		if (cacheWI != null) {
			def cid = cacheWI.id
			wiData = [method:'PATCH', uri: "/_apis/wit/workitems/${cid}?api-version=5.0-preview.3&bypassRules=true", headers: ['Content-Type': 'application/json-patch+json'], body: []]

			// Add work item type in case it changed
			def idData = [ op: 'add', path: '/fields/System.WorkItemType', value: "${map.target}"]
			wiData.body.add(idData)
			rmItemData.setCacheWI(cacheWI)
		} else {
			def idData = [ op: 'add', path: '/id', value: newId]
			newId--
			wiData.body.add(idData)
			rmItemData.setIsNew(true)
		}
		
		// Map each attribute from CLM to TFS
		map.fields.each { field ->
			def fieldData
			try {
				fieldData = getFieldData(rmItemData, field, memberMap, cacheWI, map)
			}
			catch (Exception e) {
				checkpointManagementService.addLogentry("could not getChanges for ${id} because: ${e}")
				return
			}
			if (fieldData != null) {
				if (fieldData.value != null) {
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
		
		// Cache all links so they can be processed later
		
		cacheAllLinks(id, rmItemData.links)

		
		// For all new attachments uploaded by DescriptionHandler, add associate to ADO WI
		rmItemData.adoFileInfo.each { adoFile -> 
			def change = [op: 'add', path: '/relations/-', value: [rel: "AttachedFile", url: adoFile.url, attributes:[comment: adoFile.comment]]]
			wiData.body.add(change)
		}

		if (wiData.body.size() == 1) {
			return null
		}

		return wiData

	}
	


	def getFieldData(def rmItemData, def field, def memberMap, def cacheWI, def map) {
		String handlerName = "${field.source}"
		String fValue = ""
		if (this.fieldMap["${handlerName}"] != null) {
			def data = [itemData: rmItemData, memberMap: memberMap, fieldMap: field, cacheWI: cacheWI, itemMap: map]
			def fieldData = this.fieldMap["${handlerName}"].execute(data)
			return fieldData
		}
		else {
			throw new Exception("Handler not found: ${handlerName}")
		}
		return null
	}

	def getReqMaps(String type) {
		def maps = rmMappingManagementService.getMappingData().findAll { amap ->
			"${amap.source}" == "${type}"
		}
		if (maps.size() == 0) {
			maps = rmMappingManagementService.getMappingData().findAll { amap ->
				"${amap.source}" == "Default"
			}
		}
		return maps
	}

	void getWILinkChanges(int id, String project, Closure closure) {
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
//		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
//		IWorkItemClient workItemClient = teamRepository.getClientLibrary(IWorkItemClient.class)
//		IWorkItem workItem = workItemClient.findWorkItemById(id, IWorkItem.FULL_PROFILE, null);
//		Date modified = workItem.modified()
		String sid = "${id}"
		def cacheWI = cacheManagementService.getFromCache(sid, ICacheManagementService.WI_DATA)
		if (cacheWI != null) {
			def cid = cacheWI.id
			List<LinkInfo> info = getLinkInfoFromCache(sid)
			if (info) {
				def wiData = [method:'PATCH', uri: "/_apis/wit/workitems/${cid}?api-version=5.0-preview.3", headers: ['Content-Type': 'application/json-patch+json'], body: []]
				def rev = [ op: 'test', path: '/rev', value: cacheWI.rev]
				wiData.body.add(rev)
				wiData = generateWILinkChanges(wiData, info, cacheWI)
				if (wiData.body.size() > 1) {
					closure.call('WorkItem', wiData)
				}
			} else {
				//log.debug("No links for ${sid}")
			}

		}
	}

	def generateWILinkChanges(def wiData, def links, def cacheWI) {
		//def linksList = links.split(',')
		links.each { def info ->
			String id = info.itemIdRelated
			String module = info.moduleRelated
			def url = null
			def linkType = info.type
			def linkMap = 'System.LinkTypes.Related'
			def runId = null
			def linkId = null
			def linkWI = cacheManagementService.getFromCache(id, 'TL', ICacheManagementService.WI_DATA)
			if (linkWI) {
				linkId = linkWI.id
				url = "${tfsUrl}/_apis/wit/workItems/${linkId}"
			}
			if (linkId && !linkExists(cacheWI, linkMap, linkId, runId) && "${linkId}" != "${cacheWI.id}") {
				def change = [op: 'add', path: '/relations/-', value: [rel: "${linkMap}", url: url, attributes:[comment: "Jama Link: ${linkType}"]]]
				wiData.body.add(change)
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
	private def getLinkInfoFromCache(def sid) {
		if (!sid) {
			return null
		}
		return cacheManagementService.getFromCache(sid, 'TL','LinkInfo')
	}
	private void cacheAllLinks(String itemId, def itemLinks) {
		List<LinkInfo> links = new ArrayList<LinkInfo>()
		itemLinks.each { link ->
			String type = "${jamaReptionshipTypeMap[link.relationshipType]}"
			String id = "${link.fromItem}"
			String rid = "${link.toItem}"
			def info = new LinkInfo(type: type, itemIdCurrent: id, itemIdRelated: rid, moduleCurrent: 'jama', moduleRelated: 'jama')
			links.add(info)
		}
		if (links.size() > 0) {
			cacheManagementService.saveToCache(links, "$itemId", 'LinkInfo')
		}
		return
	}


}
