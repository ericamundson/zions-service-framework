package com.zions.qm.services.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component;

import com.zions.common.services.work.handler.IFieldHandler
import groovy.json.JsonSlurper

@Component
public class ClmTestItemManagementService {
	
	@Autowired
	@Value('${cache.location}')
	String cacheLocation
	
	@Autowired
	@Value('${tfs.url}')
	String tfsUrl

	@Autowired
	private Map<String, IFieldHandler> fieldMap;

	@Autowired
	TestMappingManagementService testMappingManagementService
		
	int newId = -1
	
	
	def resetNewId() {
		newId = -1
	}

	public ClmTestItemManagementService() {
		
	}
	
	def getChanges(String project, def qmItemData, def memberMap) {
		def map = getTestMap(qmItemData)
		def outItem = []
		return generateItemData(qmItemData, map, project, memberMap)
	}
	
	def generateItemData(def qmItemData, def map, String project, def memberMap) {
		String type = map.target
		
		def etype = URLEncoder.encode(type, 'utf-8').replace('+', '%20')
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def wiData = [method:'PATCH', uri: "/${eproject}/_apis/wit/workitems/\$${etype}?api-version=5.0-preview.3&bypassRules=true", headers: ['Content-Type': 'application/json-patch+json'], body: []]
		String id = qmItemData.webId.text()
		def cacheWI = getCacheWI(id)
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
		map.fields.each { field ->
			def fieldData = getFieldData(qmItemData, field, memberMap, cacheWI, map)
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
		if (wiData.body.size() == 1) {
			return null
		}
		return wiData

	}
	
	def getFieldData(qmItemData, field, memberMap, cacheWI, map) {
		String handlerName = "${fieldMap.source}"
		String fValue = ""
		if (this.fieldMap["${handlerName}"] != null) {
			def data = [itemData: qmItemData, memberMap: memberMap, fieldMap: field, cacheWI: cacheWI, itemMap: map]
			return this.fieldMap["${handlerName}"].execute(data)
		}
		return null
	}

	String getTestMap(qmItemData) {
		String type = qmItemData.name()
		def map = testMappingManagementService.mappingData.each { amap -> 
			"${amap.source}" == "${type}"
		}
		return map
	}
	
	/**
	 * Check cache for work item state.
	 *
	 * @param id
	 * @return
	 */
	def getCacheWI(id) {
		File cacheData = new File("${this.cacheLocation}${File.separator}${id}${File.separator}wiData.json");
		if (cacheData.exists()) {
			JsonSlurper s = new JsonSlurper()
			return s.parse(cacheData)
		}
		return null

	}

}
