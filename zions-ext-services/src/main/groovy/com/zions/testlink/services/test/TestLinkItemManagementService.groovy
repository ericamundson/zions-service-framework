package com.zions.testlink.services.test

import br.eti.kinoshita.testlinkjavaapi.model.TestCase
import br.eti.kinoshita.testlinkjavaapi.model.TestPlan
import br.eti.kinoshita.testlinkjavaapi.model.TestSuite
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.zions.common.services.cache.ICacheManagementService
import groovyx.net.http.ContentType

/**
 * Handles generating data required to send to ADO to update MS Test Manager with test
 * plan data.
 * 
 * @author z091182
 *
 */
@Component
class TestLinkItemManagementService {
	@Autowired
	@Value('${cache.location}')
	String cacheLocation
	
	@Autowired
	@Value('${tfs.url}')
	String tfsUrl
	
	@Autowired
	ICacheManagementService cacheManagementService

	@Autowired
	TestLinkMappingManagementService testLinkMappingManagementService

	@Autowired
	TestLinkClient testLinkClient
	
	int newId = -1
	
	def resetNewId() {
		newId = -1
	}

	public TestLinkItemManagementService() {
		
	}

	/**
	 * Main entry point for generating ADO change requests
	 *
	 * @param project
	 * @param qmItemData
	 * @param memberMap
	 * @return
	 */
	void processForChanges(String project, def tlItemData, def memberMap, def resultMap = null, def testCase = null, def parent = null, Closure closure) {
		def maps = getTestMaps(tlItemData)
		def outItems = [:]
		maps.each { map ->
			def item = null;
			if ("${map.target}" == 'Result') {
				item = generateExecutionData(tlItemData, map, project, memberMap, resultMap, testCase)
//			} else if ("${map.target}" == 'Configuration') {
//				item = generateConfigurationData(tlItemData, map, project, memberMap)
			} else if ("${map.target}" == 'Test Case' || "${map.target}".contains(' WI')){
				item = generateWorkItemData(tlItemData, map, project, memberMap, parent)
			} else if ("${map.target}" == 'Test Plan'){
				item = generateTestPlanData(tlItemData, map, project, memberMap, parent)
			} else if ("${map.target}" == 'Test Suite'){
				item = generateTestSuiteData(tlItemData, map, project, memberMap, parent)
			}
			String key = "${map.target}"
			if (item) {
				closure(key, item)
			}
		}
	}
	
	private def generateExecutionData(def tlItemData, def map, String project, def memberMap, def resultMap, def testCase) {
		String type = map.target
		def etype = URLEncoder.encode(type, 'utf-8').replace('+', '%20')
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def exData = [:]
		String id = "${tlItemData.webId.text()}"
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
			def fieldData = getFieldData(tlItemData, id, field, memberMap, cacheResult, map, resultMap, testCase)
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
	
	private def getResultData(def resultMap, def testCase) {
		String rqmId = "${testCase.webId.text()}-Test Case"
		def adoTestCase = cacheManagementService.getFromCache(rqmId, ICacheManagementService.WI_DATA)
		if (adoTestCase == null) return null
		return resultMap["${adoTestCase.id}"]
	}

	
	private def generateWorkItemData(def tlItemData, def map, String project, def memberMap, def parent = null) {
		String type = map.target
		def etype = URLEncoder.encode(type, 'utf-8').replace('+', '%20')
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def wiData = [:]
		String id = "${qmItemData.webId.text()}-${type}"
		def cacheWI = null
		if (type.endsWith(' WI')) {
			String atype = "${map.target}".substring(0, type.length()-3)
			etype = URLEncoder.encode(atype, 'utf-8').replace('+', '%20')
		}
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
		map.fields.each { field ->
			def fieldData = getFieldData(tlItemData, id, field, memberMap, cacheWI, map)
			if (fieldData != null) {
				if (type != 'Test Case' && !type.endsWith(' WI')) {
					if (fieldData.value != null) {
						wiData.body["${field.target}"] = fieldData.value
					}
				} else {
					if (!(fieldData instanceof List)) {
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
	
	private def generateTestPlanData(def tlItemData, def map, String project, def memberMap, def parent = null) {
		String type = map.target
		def etype = URLEncoder.encode(type, 'utf-8').replace('+', '%20')
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def wiData = [:]
		String id = "${qmItemData.webId.text()}-${type}"
		def cacheWI = cacheManagementService.getFromCache(id, ICacheManagementService.PLAN_DATA)
		wiData = [method: 'post', requestContentType: ContentType.JSON, contentType: ContentType.JSON, uri: "/${eproject}/_apis/test/plans", query:['api-version':'5.0-preview.2'], body: [:]]
		if (cacheWI != null) {
			def cid = cacheWI.id
			wiData = [method:'patch', requestContentType: ContentType.JSON, contentType: ContentType.JSON, uri: "/${eproject}/_apis/test/plans/${cid}", query:['api-version':'5.0-preview.2'], body: [:]]
		}
		map.fields.each { field ->
			def fieldData = getFieldData(qmItemData, id, field, memberMap, cacheWI, map)
			if (fieldData != null) {
				if (type != 'Test Case' && !type.endsWith(' WI')) {
					if (fieldData.value != null) {
						wiData.body["${field.target}"] = fieldData.value
					}
				} else {
					if (!(fieldData instanceof List)) {
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
		if (wiData.body.size() == 0) {
			return null
		}
		return wiData

	}
	
	private def generateTestSuiteData(def tlItemData, def map, String project, def memberMap, def parent = null) {
		String type = map.target
		def etype = URLEncoder.encode(type, 'utf-8').replace('+', '%20')
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def wiData = [:]
		String id = "${qmItemData.webId.text()}-${type}"
		def cacheWI = cacheManagementService.getFromCache(id, ICacheManagementService.SUITE_DATA)
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
		map.fields.each { field ->
			def fieldData = getFieldData(qmItemData, id, field, memberMap, cacheWI, map)
			if (fieldData != null) {
				if (type != 'Test Case' && !type.endsWith(' WI')) {
					if (fieldData.value != null) {
						wiData.body["${field.target}"] = fieldData.value
					}
				} else {
					if (!(fieldData instanceof List)) {
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
		if (wiData.body.size() == 0) {
			return null
		}
		return wiData

	}
	
	private def getFieldData(def qmItemData, def id, def field, def memberMap, def cacheWI, def map, def resultMap = null, def testCase = null) {
		String handlerName = "${field.source}"
		String qmHandlerName = "Tl${handlerName.substring(0,1).toUpperCase()}${handlerName.substring(1)}"
		String fValue = ""
		if (this.fieldMap[qmHandlerName] != null) {
			def data = [itemData: qmItemData, id: id, memberMap: memberMap, fieldMap: field, cacheWI: cacheWI, itemMap: map, resultMap: resultMap, testCase: testCase]
			if (testCase != null) {
				data['testCase'] = testCase
			}
			if (resultMap != null) {
				data['resultMap'] = resultMap
			}
			def fieldData = this.fieldMap[qmHandlerName].execute(data)
			return fieldData
		} else if (this.fieldMap["${handlerName}"] != null) {
			def data = [itemData: qmItemData, id: id, memberMap: memberMap, fieldMap: field, cacheWI: cacheWI, itemMap: map, resultMap: resultMap, testCase: testCase]
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
	
	private def getTestMaps(tlItemData) {
		String type = "${tlItemData.name()}"
		def maps = testLinkMappingManagementService.mappingData.findAll { amap ->
			"${amap.source}" == "${type}"
		}
		return maps
	}
	
	public def setParent(def parent, def children, def map, Closure c) {
		String pid = "${parent.id}"
		String type = ICacheManagementService.PLAN_DATA
		if (parent instanceof TestSuite) type = ICacheManagementService.SUITE_DATA
		def parentData = cacheManagementService.getFromCache(pid, type)
		if (parentData != null) {
			def tcIds = []
			int tot = children.size()
			int count = 0
			children.each { child ->
				
				
				String cid = "${child.id}"
				def childData = cacheManagementService.getFromCache(cid, ICacheManagementService.WI_DATA)
				if (childData != null) {
					tcIds.add("${childData.id}")
				}
				if (tcIds.size() == 5 || (count+1==tot && tcIds.size() > 0)) {
					c.call(parentData, tcIds)
					tcIds = []
				}
				count++
			}
			
		}
		
	}
	private String getTargetName(String name, def map) {
		def maps = map.findAll { amap ->
			"${amap.source}" == "${name}"
		}
		if (maps.size() > 0) {
			String retVal = "${maps[0].target}"
			return retVal
		}
		return null

	}


}
