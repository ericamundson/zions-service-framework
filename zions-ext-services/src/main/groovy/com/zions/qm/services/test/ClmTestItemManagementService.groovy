package com.zions.qm.services.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component;

import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.util.ObjectUtil
import com.zions.common.services.work.handler.IFieldHandler

import groovy.json.JsonSlurper
import groovy.xml.XmlUtil
import groovyx.net.http.ContentType

/**
 * Class responsible for processing RQM test planning data to generate Azure Devops test data.
 * 
 * <p>Design</p>
 * <img src="ClmTestItemManagementService.png"/>
 * 
 * @author z091182
 * 
 * @startuml
 * class ClmTestItemManagementService {
 *  ... Get ADO Test Plan/Execution field data ...
 *  + getChanges(String project, def qmItemData, def memberMap, def runData = null, def testCase = null)
 * }
 * note left: @Component
 * 
 * class Map<String, IFieldHandler> {
 * }
 * note left: String is name of handler Class, IFieldHandler is actual class
 * 
 * ClmTestItemManagementService --> Map: @Autowired fieldMap - The data conversion handlers
 * ClmTestItemManagementService --> TestMappingManagementService: @Autowired testMappingManagementService - Manages field in to out map data.
 * @enduml
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
	ICacheManagementService cacheManagementService

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
	def getChanges(String project, def qmItemData, def memberMap, def resultMap = null, def testCase = null, def parent = null) {
		def maps = getTestMaps(qmItemData)
		def outItems = [:]
		maps.each { map ->
			if ("${map.target}" == 'Result') {
				def item = generateExecutionData(qmItemData, map, project, memberMap, resultMap, testCase)
				if (item != null) {
					outItems["${map.target}"] = item
				}

			} else if ("${map.target}" == 'Configuration') {
				def item = generateConfigurationData(qmItemData, map, project, memberMap)
				if (item != null) {
					outItems["${map.target}"] = item
				}

			}else {
				def item = generateItemData(qmItemData, map, project, memberMap, parent)
				if (item != null) {
					outItems["${map.target}"] = item
				}
			}
		}
		return outItems
	}
	
	/**
	 * Generates ADO test result information from mapping.
	 * 
	 * @param qmItemData - RQM executionresult data
	 * @param map - Field map data
	 * @param project - ADO project name
	 * @param memberMap - ADO project member map
	 * @param resultMap - ADO map of test case id to result
	 * @param testCase - RQM testcase data
	 * @return ADO 'Result' rest object
	 */
	private def generateExecutionData(def qmItemData, def map, String project, def memberMap, def resultMap, def testCase) {
		String type = map.target
		def etype = URLEncoder.encode(type, 'utf-8').replace('+', '%20')
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def exData = [:]
		String id = "${qmItemData.webId.text()}-${map.target}"
		def cacheResult = getResultData(resultMap, testCase)
		if (!cacheResult) return null
		String runId = "${cacheResult.testRun.id}"
		exData = [method: 'post', requestContentType: ContentType.JSON, contentType: ContentType.JSON, uri: "/${eproject}/_apis/test/Runs/${runId}/results", query:['api-version':'5.0-preview.5'], body: []]
		if (cacheResult != null) {
			def cid = cacheResult.id
			exData = [method:'patch', requestContentType: ContentType.JSON, contentType: ContentType.JSON, uri: "/${eproject}/_apis/test/Runs/${runId}/results/${cid}", query:['api-version':'5.0-preview.5'], body: []]
		}
		def bodyItem = [:]
		map.fields.each { field ->
			def fieldData = getFieldData(qmItemData, field, memberMap, cacheResult, map, resultMap, testCase)
			if (fieldData != null) {
				if (fieldData.value != null) {
					bodyItem["${field.target}"] = fieldData.value
				}
			}
			
		}
		if (bodyItem.size() == 0) {
			return null
		}
		exData.body.add(bodyItem)
		return exData
	}
	
	
	private def generateConfigurationData(def qmItemData, def map, String project, def memberMap) {
		String type = map.target
		def etype = URLEncoder.encode(type, 'utf-8').replace('+', '%20')
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def exData = [:]
		String id = "${qmItemData.name.text()}-${map.target}"
		def cacheConfig = cacheManagementService.getFromCache(id, ICacheManagementService.CONFIGURATION_DATA)

		exData = [method: 'post', requestContentType: ContentType.JSON, contentType: ContentType.JSON, uri: "/${eproject}/_apis/test/Configurations", query:['api-version':'5.0-preview.2'], body: []]
		if (cacheConfig != null) {
			def cid = cacheConfig.id
			exData = [method:'patch', requestContentType: ContentType.JSON, contentType: ContentType.JSON, uri: "/${eproject}/_apis/test/Configurations/${cid}", query:['api-version':'5.0-preview.2'], body: []]
		}
		def bodyItem = [:]
		map.fields.each { field ->
			def fieldData = getFieldData(qmItemData, field, memberMap, cacheConfig, map)
			if (fieldData != null) {
				if (fieldData.value != null) {
					bodyItem["${field.target}"] = fieldData.value
				}
			}
			
		}
		if (bodyItem.size() == 0) {
			return null
		}
		if (!bodyItem['values']) {
			bodyItem['values'] = []
		
		}
		bodyItem['isDefault'] = false
		exData.body = bodyItem
		
		return exData
	}

	private def getResultData(def resultMap, def testCase) {
		String rqmId = "${testCase.webId.text()}-Test Case"
		def adoTestCase = cacheManagementService.getFromCache(rqmId, ICacheManagementService.WI_DATA)
		if (adoTestCase == null) return null
		return resultMap["${adoTestCase.id}"]
	}
	
	private def generateItemData(def qmItemData, def map, String project, def memberMap, def parent = null) {
		String type = map.target
		def etype = URLEncoder.encode(type, 'utf-8').replace('+', '%20')
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def wiData = [:]
		String id = "${qmItemData.webId.text()}-${map.target}"
		def cacheWI = null
		if (type == 'Test Case') {
			cacheWI = cacheManagementService.getFromCache(id, ICacheManagementService.WI_DATA)
			wiData = [method:'PATCH', uri: "/${eproject}/_apis/wit/workitems/\$${etype}?api-version=5.0-preview.3&bypassRules=true", headers: ['Content-Type': 'application/json-patch+json'], body: []]
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
		} else if (type == 'Test Plan'){
			cacheWI = cacheManagementService.getFromCache(id, ICacheManagementService.PLAN_DATA)
			wiData = [method: 'post', requestContentType: ContentType.JSON, contentType: ContentType.JSON, uri: "/${eproject}/_apis/test/plans", query:['api-version':'5.0-preview.2'], body: [:]]
			if (cacheWI != null) {
				def cid = cacheWI.id
				wiData = [method:'patch', requestContentType: ContentType.JSON, contentType: ContentType.JSON, uri: "/${eproject}/_apis/test/plans/${cid}", query:['api-version':'5.0-preview.2'], body: [:]]
			}
		} else if (type == 'Test Suite'){
			cacheWI = cacheManagementService.getFromCache(id, ICacheManagementService.SUITE_DATA)
			if (parent != null) {
				String parentId = "${parent.id}"
				String cid = "${parent.rootSuite.id}"
				wiData = [method: 'post', requestContentType: ContentType.JSON, contentType: ContentType.JSON, uri: "/${eproject}/_apis/test/plans/${parentId}/suites/${cid}", query:['api-version':'5.0-preview.3'], body: [:]]
				wiData.body.parent = parent.rootSuite
				if (cacheWI != null) {
					cid = cacheWI.id
					wiData = [method:'patch', requestContentType: ContentType.JSON, contentType: ContentType.JSON, uri: "/${eproject}/_apis/test/plans/${parentId}/suites/${cid}", query:['api-version':'5.0-preview.3'], body: [:]]
				}
			}
		}
		
		map.fields.each { field ->
			def fieldData = getFieldData(qmItemData, field, memberMap, cacheWI, map)
			if (fieldData != null) {
				if (type != 'Test Case') {
					if (fieldData.value != null) {
						wiData.body["${field.target}"] = fieldData.value
					}
				} else {
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
			
		}
		if (type != 'Test Case') {
			if (wiData.body.size() == 0) {
				return null
			}
		} else {
			if (wiData.body.size() == 1) {
				return null
			}

		}
		return wiData

	}
	
	private def getFieldData(def qmItemData, def field, def memberMap, def cacheWI, def map, def resultMap = null, def testCase = null) {
		String handlerName = "${field.source}"
		String fValue = ""
		if (this.fieldMap["${handlerName}"] != null) {
			def data = [itemData: qmItemData, memberMap: memberMap, fieldMap: field, cacheWI: cacheWI, itemMap: map, resultMap: resultMap, testCase: testCase]
			if (testCase != null) {
				data['testCase'] = testCase
			}
			if (resultMap != null) {
				data['resultMap'] = resultMap
			}
			def fieldData = this.fieldMap["${handlerName}"].execute(data)
			return fieldData
		}
		return null
	}

	private def getTestMaps(qmItemData) {
		String type = "${qmItemData.name()}"
		def maps = testMappingManagementService.mappingData.findAll { amap -> 
			"${amap.source}" == "${type}"
		}
		return maps
	}
	

}
