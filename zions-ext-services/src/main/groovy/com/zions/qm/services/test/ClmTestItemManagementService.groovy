package com.zions.qm.services.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component;
import com.zions.common.services.util.ObjectUtil
import com.zions.common.services.work.handler.IFieldHandler
import groovy.json.JsonSlurper
import groovy.xml.XmlUtil
import groovyx.net.http.ContentType

/**
 * Class responsible for processing RQM test planning data to generate Azure Devops test data.
 * 
 * @author z091182
 *
 */
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
	
	/**
	 * Main entry point for generating requests
	 * 
	 * @param project
	 * @param qmItemData
	 * @param memberMap
	 * @return
	 */
	def getChanges(String project, def qmItemData, def memberMap) {
		def maps = getTestMaps(qmItemData)
		def outItems = [:]
		maps.each { map ->
			def item = generateItemData(qmItemData, map, project, memberMap)
			if (item != null) {
				outItems["${map.target}"] = item
			}
		}
		return outItems
	}
	
	def generateItemData(def qmItemData, def map, String project, def memberMap) {
		String type = map.target
		def etype = URLEncoder.encode(type, 'utf-8').replace('+', '%20')
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def wiData = [:]
		String id = "${qmItemData.webId.text()}-${map.target}"
		def cacheWI = getCacheWI(id)
		if (type != 'Test Plan') {
			wiData = [method:'PATCH', uri: "/${eproject}/_apis/wit/workitems/\$${etype}?api-version=5.0-preview.3&bypassRules=true", headers: ['Content-Type': 'application/json-patch+json'], body: []]
			if (cacheWI != null) {
				def cid = cacheWI.id
				wiData = [method:'PATCH', uri: "/_apis/wit/workitems/${cid}?api-version=5.0-preview.3&bypassRules=true", headers: ['Content-Type': 'application/json-patch+json'], body: [:]]
				def rev = [ op: 'test', path: '/rev', value: cacheWI.rev]
				wiData.body.add(rev)
			} else {
				def idData = [ op: 'add', path: '/id', value: newId]
				newId--
				wiData.body.add(idData)
			}
		} else {
			wiData = [method: 'post', requestContentType: ContentType.JSON, contentType: ContentType.JSON, uri: "/${eproject}/_apis/testplan/plans", query:['api-version':'5.0-preview.1'], body: [:]]
			if (cacheWI != null) {
				def cid = cacheWI.id
				wiData = [method:'patch', requestContentType: ContentType.JSON, contentType: ContentType.JSON, uri: "/${eproject}/_apis/testplan/plans/${cid}", query:['api-version':'5.0-preview.1'], body: [:]]
			}
		}
		
		map.fields.each { field ->
			def fieldData = getFieldData(qmItemData, field, memberMap, cacheWI, map)
			if (fieldData != null) {
				if (type != 'Test Plan') {
					if (fieldData.value != null) {
						wiData.body.add(fieldData)
					} else {
						fieldData.each { fData ->
							if (fData.value != null) {
								wiData.body.add(fData)
							}
						}
					}
				} else {
					if (fieldData.value != null) {
						wiData.body["${field.target}"] = fieldData.value
					}
				}
			}
			
		}
		if (type != 'Test Plan') {
			if (wiData.body.size() == 1) {
				return null
			}
		} else {
			if (wiData.body.size() == 0) {
				return null
			}

		}
		return wiData

	}
	
	def getFieldData(def qmItemData, def field, def memberMap, def cacheWI, def map) {
		String handlerName = "${field.source}"
		String fValue = ""
		if (this.fieldMap["${handlerName}"] != null) {
			def data = [itemData: qmItemData, memberMap: memberMap, fieldMap: field, cacheWI: cacheWI, itemMap: map]
			def fieldData = this.fieldMap["${handlerName}"].execute(data)
			if (fieldData != null) {
				String val = "${fieldData.'value'}"
				if (field.defaultValue != null) {
					val = "${field.defaultValue}"
				}
				if (field.values.size() > 0) {
					
					field.values.each { aval ->
						if ("${fValue}" == "${aval.source}") {
							val = "${aval.target}"
							return
						}
					}
				}
				fieldData.'value' = val
			}
			return fieldData
		}
		return null
	}

	def getTestMaps(qmItemData) {
		String type = "${qmItemData.name()}"
		def maps = testMappingManagementService.mappingData.findAll { amap -> 
			"${amap.source}" == "${type}"
		}
		return maps
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
