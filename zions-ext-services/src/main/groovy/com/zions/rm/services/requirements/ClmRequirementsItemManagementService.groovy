package com.zions.rm.services.requirements

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component;

import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.util.ObjectUtil
import com.zions.common.services.work.handler.IFieldHandler
import com.zions.rm.services.requirements.RequirementsMappingManagementService
import com.zions.rm.services.requirements.RequirementQueryData

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
class ClmRequirementsItemManagementService {
	@Autowired
	@Value('${cache.location}')
	String cacheLocation
	
	@Autowired
	@Value('${tfs.url}')
	String tfsUrl

	@Autowired
	private Map<String, IFieldHandler> fieldMap;
	
	@Autowired
	RequirementsMappingManagementService rmMappingManagementService
	
	@Autowired(required=true)
	ICacheManagementService cacheManagementService
	
	int newId = -1
	
	
	def resetNewId() {
		newId = -1
	}
	public ClmRequirementsItemManagementService() {
		// TODO Auto-generated constructor stub
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
		} else {
			def idData = [ op: 'add', path: '/id', value: newId]
			newId--
			wiData.body.add(idData)
		}
		
		// Map each attribute from CLM to TFS
		map.fields.each { field ->
			def fieldData = getFieldData(rmItemData, field, memberMap, cacheWI, map)
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
		
		// For wrapped resource, associate attachments (uploaded by DescriptionHandler)
		if (rmItemData.adoFileInfo.size() > 0) {
			String comment = "Migrated from Rational DNG"
			rmItemData.adoFileInfo.each { adoFile -> 
				def change = [op: 'add', path: '/relations/-', value: [rel: "AttachedFile", url: adoFile.url, attributes:[comment: comment]]]
				wiData.body.add(change)
			}
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


}
