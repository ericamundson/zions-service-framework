package com.zions.qm.services.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component;

import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cacheaspect.Cache
import com.zions.common.services.cacheaspect.CacheWData
import com.zions.common.services.link.LinkInfo
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.util.ObjectUtil
import com.zions.common.services.work.handler.IFieldHandler
import com.zions.qm.services.test.handlers.QmBaseAttributeHandler
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import groovy.xml.XmlUtil
import groovyx.net.http.ContentType
import com.zions.common.services.cacheaspect.CacheInterceptor


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
 *  + processForChanges(String project, def qmItemData, def memberMap, def resultMap = null, def testCase = null, def parent = null, Closure closure)
 * }
 * note left: @Component
 * 
 * class Map<String, QmBaseAttributeHandler> {
 * }
 * note left: String is name of handler Class, IFieldHandler is actual class
 * 
 * ClmTestItemManagementService --> Map: @Autowired fieldMap - The data conversion handlers
 * ClmTestItemManagementService --> TestMappingManagementService: @Autowired testMappingManagementService - Manages field in to out map data.
 * ClmTestItemManagementService --> ICacheManagementService: @Autowired cacheManagementService - Store work item data.
 * @enduml
 *
 */
@Component
@Slf4j
public class ClmTestItemManagementService {
	
	@Autowired
	@Value('${cache.location}')
	String cacheLocation
	
	@Autowired
	@Value('${tfs.url}')
	String tfsUrl
	
	@Autowired
	ICacheManagementService cacheManagementService

	@Autowired(required=false)
	Map<String, QmBaseAttributeHandler> fieldMap;

	@Autowired
	TestMappingManagementService testMappingManagementService
	
	@Value('${check.updated:false}')
	boolean checkUpdated
	
	@Value('${update.cache.only:false}')
	boolean updateCacheOnly
	
	@Autowired
	IGenericRestClient qmGenericRestClient
		
	int newId = -1
	
	Map<String, String> itemMap = ['testcase': 'Test Case', 'testsuite': 'Test Suite', 'testplan': 'Test Plan']
	
	Map<String, String> wiNameMap = ['testcase': 'Test Case', 'testsuite': 'Test Suite WI', 'testplan': 'Test Suite WI']
	//Map<String, String> wiNameMap = ['testcase': 'Test Case', 'testsuite': 'Test Suite WI', 'testplan': 'Test Plan WI']
	
	
	def resetNewId() {
		newId = -1
	}
	
	@Value('${refresh.run:false}')
	boolean refreshRun

	public ClmTestItemManagementService() {
		
	}
	
	/**
	 * Called while access data elements. To cache link data prior to creating links.
	 * 
	 * @param qmItemData - RQM element data.
	 */
	void cacheLinkChanges(def qmItemData) {
		String iType = "${qmItemData.name()}"
		String oType = wiNameMap[iType]
		String sid = "${qmItemData.webId.text()}-${oType}"
		String tss = "${qmItemData.updated.text()}"
		Date ts = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", tss)
		new CacheInterceptor() {}.provideCaching(this, sid, ts, LinkHolder) {
			List<LinkInfo> links = getAllLinks(sid, ts, qmItemData)
		}

	}
	
	String resolveId(String cId, String module) {
		String id = null
		if (cId.startsWith('http')) {
			try {
				String url = cId
				def result = qmGenericRestClient.get(
				contentType: ContentType.XML,
				uri: cId,
				headers: [Accept: 'application/rdf+xml'] );
				if (!result) {
					return null
				}
				//println new XmlUtil().serialize(result)
				if (module == 'CCM') {
					def identifier = result.'**'.find { node ->
				
						node.name() == 'identifier'
					}
//					String xml = new XmlUtil().serialize(result)
//					File outXml = new File('clm.xml')
//					def os = outXml.newDataOutputStream()
//					os << xml
//					os.close()
					//id = "${identifier.text()}"
					id = null
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

	
	/**
	 * Entry point for processing ADO work item link changes.
	 * 
	 * @param qmItemData - CLM RQM element data
	 * @param closure - callback for processing submission to ADO.
	 */
	void processForLinkChanges(def qmItemData, Closure closure) {
		String iType = "${qmItemData.name()}"
		String oType = wiNameMap[iType]
		String sid = "${qmItemData.webId.text()}-${oType}"
		String tss = "${qmItemData.updated.text()}"
		def cacheWI = cacheManagementService.getFromCache(sid, ICacheManagementService.WI_DATA)
		if (!cacheWI) return
		Date ts = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", tss)
		List<LinkInfo> links = []
		//new CacheInterceptor() {}.provideCaching(this, sid, ts, LinkHolder) {
		links = getAllLinks(sid, ts, qmItemData)
		//}
		if (links.size() == 0) return
		String cid = "${cacheWI.id}"
		def wiData = [method:'PATCH', uri: "/_apis/wit/workitems/${cid}?api-version=5.0-preview.3", headers: ['Content-Type': 'application/json-patch+json'], body: []]
		def rev = [ op: 'test', path: '/rev', value: cacheWI.rev]
		wiData.body.add(rev)
		links.each { LinkInfo info ->
			String id = info.itemIdRelated
			String module = info.moduleRelated
			id = resolveId(id, module)
			def url = null
			def runId = null
			def linkId = null
			def linkWI = null
			if (id) {
				linkWI = cacheManagementService.getFromCache(id, module, ICacheManagementService.WI_DATA)
			}
			if (linkWI) {
				linkId = linkWI.id
				url = "${tfsUrl}/_apis/wit/workItems/${linkId}"
			}
			if (linkId && !linkExists(cacheWI, linkId) && "${linkId}" != "${cacheWI.id}") {
				def change = [op: 'add', path: '/relations/-', value: [rel: 'System.LinkTypes.Related', url: url, attributes:[comment: "${info.type}"]]]
				wiData.body.add(change)
			}

		}
		if (wiData.body.size() > 1) {
			closure(oType, wiData)
		}
	}

	private boolean linkExists(cacheWI, linkId) {
		def url = "${tfsUrl}/_apis/wit/workItems/${linkId}"
		def link = cacheWI.relations.find { rel ->
			url == "${rel.url}"
		}
		return link != null
	}

	/**
	 * Main entry point for generating requests
	 * 
	 * @param project
	 * @param qmItemData
	 * @param memberMap
	 * @return
	 */
	void processForChanges(String project, def qmItemData, def memberMap, def resultMap = null, def testCase = null, def parent = null, def exData = null, Closure closure) {
		def maps = getTestMaps(qmItemData)
		def outItems = [:]
		maps.each { map ->
			def item = null;
			if ("${map.target}" == 'Result') {
				item = generateExecutionData(qmItemData, map, project, memberMap, resultMap, testCase, exData)

			} else if ("${map.target}" == 'Configuration') {
				item = generateConfigurationData(qmItemData, map, project, memberMap)

			} else {
				item = generateItemData(qmItemData, map, project, memberMap, parent)
			}
			String key = "${map.target}"
			if (item) {
				closure(key, item)
			}
		}
		//return outItems
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
	private def generateExecutionData(def qmItemData, def map, String project, def memberMap, def resultMap, def testCase, def inExData = null) {
		String type = map.target
		def etype = URLEncoder.encode(type, 'utf-8').replace('+', '%20')
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def exData = [:]
		String id = "${qmItemData.webId.text()}"
		String cacheId = "${id}-Result"
		if (refreshRun) {
			cacheManagementService.deleteByIdAndByType(cacheId, 'resultAttachments')
		}
//		def cacheData = cacheManagementService.getFromCache(cacheId, ICacheManagementService.RESULT_DATA)
		def cacheResult = getResultData(resultMap, testCase)
		if (!cacheResult) return null
		String runId = "${cacheResult.testRun.id}"
		if (inExData) {
			exData = inExData
		} else {
			
			exData = [method: 'post', requestContentType: ContentType.JSON, contentType: ContentType.JSON, uri: "/${eproject}/_apis/test/Runs/${runId}/results", query:['api-version':'5.0'], body: []]
			if (cacheResult) {
				def cid = cacheResult.id
				exData = [method:'patch', requestContentType: ContentType.JSON, contentType: ContentType.JSON, uri: "/${eproject}/_apis/test/Runs/${runId}/results", query:['api-version':'5.0'], body: []]
			}
		}
		def bodyItem = [:]
		map.fields.each { field ->
			def fieldData = getFieldData(qmItemData, id, field, memberMap, cacheResult, map, resultMap, testCase, null, exData)
			if (fieldData != null) {
				if (fieldData.value != null) {
					bodyItem["${field.target}"] = fieldData.value
				}
			}
			
		}
		if (bodyItem.size() == 0) {
			return null
		}
		String iId = "${bodyItem.id}"
		def bodyOut = exData.body.findAll { item ->
			String cId = "${item.id}"
			cId == iId
		}
		//String outcome = "${bodyItem.outcome}"
		if (!bodyOut || bodyOut.size() == 0) {
			String oBody = new JsonBuilder([bodyItem]).toPrettyString()
			exData.body = oBody
			//exData.body.add(bodyItem)
		}
		return exData
	}
	
	
	private def generateConfigurationData(def qmItemData, def map, String project, def memberMap) {
		String type = map.target
		def etype = URLEncoder.encode(type, 'utf-8').replace('+', '%20')
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def exData = [:]
		String id = "${qmItemData.name.text()}-${type}"
		def cacheConfig = cacheManagementService.getFromCache(id, ICacheManagementService.CONFIGURATION_DATA)
		if (!cacheConfig && updateCacheOnly) return null
		
		exData = [method: 'post', requestContentType: ContentType.JSON, contentType: ContentType.JSON, uri: "/${eproject}/_apis/test/Configurations", query:['api-version':'5.0-preview.2'], body: []]
		if (cacheConfig != null) {
			def cid = cacheConfig.id
			exData = [method:'patch', requestContentType: ContentType.JSON, contentType: ContentType.JSON, uri: "/${eproject}/_apis/test/Configurations/${cid}", query:['api-version':'5.0-preview.2'], body: []]
		}
		def bodyItem = [:]
		map.fields.each { field ->
			def fieldData = getFieldData(qmItemData, id, field, memberMap, cacheConfig, map)
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
	
	public boolean isModified(Object item, def cacheWI) {
		if (!cacheWI) return true
		if (!checkUpdated) return true
		String sDate = "${item.updated.text()}"
		sDate = sDate.substring(0, 19)
		Date clmDate = new Date().parse("yyyy-MM-dd'T'HH:mm:ss", sDate);
		sDate = "${cacheWI.fields.'System.ChangedDate'}"
		sDate = sDate.substring(0, 19)
		Date adoDate = new Date().parse("yyyy-MM-dd'T'HH:mm:ss", sDate);
		return clmDate.time >= adoDate.time;
	}

	
	private def generateItemData(def qmItemData, def map, String project, def memberMap, def parent = null) {
		String type = map.target
		def etype = URLEncoder.encode(type, 'utf-8').replace('+', '%20')
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def wiData = [:]
		String id = "${qmItemData.webId.text()}-${type}"
		def cacheWI = null
		def prevWI = null
		if (type == 'Test Case' || type.endsWith(' WI')) {
			if (type.endsWith(' WI')) {
				String atype = "${map.target}".substring(0, type.length()-3)
				etype = URLEncoder.encode(atype, 'utf-8').replace('+', '%20')
			}
			cacheWI = cacheManagementService.getFromCache(id, ICacheManagementService.WI_DATA)
			prevWI = cacheManagementService.getFromCache(id, 'wiPrevious')
			if (!cacheWI && updateCacheOnly) return null
			if (type == 'Test Case' && !isModified(qmItemData, cacheWI)) return null
			wiData = [method:'PATCH', uri: "/${eproject}/_apis/wit/workitems/\$${etype}?api-version=5.0&bypassRules=true&suppressNotifications=true", headers: ['Content-Type': 'application/json-patch+json'], body: []]
			if (cacheWI != null) {
				def cid = cacheWI.id
				wiData = [method:'PATCH', uri: "/_apis/wit/workitems/${cid}?api-version=5.0&bypassRules=true&suppressNotifications=true", headers: ['Content-Type': 'application/json-patch+json'], body: []]
				def rev = [ op: 'test', path: '/rev', value: cacheWI.rev]
				wiData.body.add(rev)
			} else {
				def idData = [ op: 'add', path: '/id', value: newId]
				newId--
				wiData.body.add(idData)
			}
		} else if (type == 'Test Plan'){
			cacheWI = cacheManagementService.getFromCache(id, ICacheManagementService.PLAN_DATA)
			if (!cacheWI && updateCacheOnly) return null
			wiData = [method: 'post', requestContentType: ContentType.JSON, contentType: ContentType.JSON, uri: "/${eproject}/_apis/test/plans", query:['api-version':'5.0-preview.2'], body: [:]]
			if (cacheWI != null) {
				def cid = cacheWI.id
				wiData = [method:'patch', requestContentType: ContentType.JSON, contentType: ContentType.JSON, uri: "/${eproject}/_apis/test/plans/${cid}", query:['api-version':'5.0-preview.2'], body: [:]]
			}
		} else if (type == 'Test Suite'){
			cacheWI = cacheManagementService.getFromCache(id, ICacheManagementService.SUITE_DATA)
			if (!cacheWI && updateCacheOnly) return null
			if (parent != null) {
				String cid = ''
				String parentId = ''
				if (parent.rootSuite) {
					parentId = "${parent.id}"
					cid = "${parent.rootSuite.id}"
				} else {
					parentId = "${parent.plan.id}"
					cid = "${parent.id}"
				}
				wiData = [method: 'post', requestContentType: ContentType.JSON, contentType: ContentType.JSON, uri: "/${eproject}/_apis/test/plans/${parentId}/suites/${cid}", query:['api-version':'5.0'], body: [:]]
				if (parent.rootSuite) {
					wiData.body.parent = parent.rootSuite
				} else {
					wiData.body.parent = [:]
					wiData.body.parent.id = parent.id
					wiData.body.parent.name = parent.name
					wiData.body.parent.url = parent.url
					
					
				}
				if (cacheWI != null) {
					cid = cacheWI.id
					wiData = [method:'patch', requestContentType: ContentType.JSON, contentType: ContentType.JSON, uri: "/${eproject}/_apis/test/plans/${parentId}/suites/${cid}", query:['api-version':'5.0'], body: [:]]
				}
				wiData.body.suiteType = 'StaticTestSuite'
			}
		}
		
		map.fields.each { field ->
			if (canChange(prevWI, cacheWI, field, id)) {
				def fieldData = getFieldData(qmItemData, id, field, memberMap, cacheWI, map, null, null, prevWI)
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
	
	boolean canChange(prevWI, cacheWI, field, String key) {
		if (!cacheWI) return true
		boolean flag = true
		String tName = "${field.target}"
		def fModified = cacheManagementService.getFromCache("${key}-${tName}", 'changedField')
		if (fModified) {
			def cVal = cacheWI.fields."${tName}"
			String changedDate = "${cacheWI.fields.'System.ChangedDate'}"
			cacheManagementService.saveToCache([changeDate: changedDate, value: cVal], "${key}-${tName}", 'changedField')
			return false
		}
		if (!prevWI) return true
		def cVal = cacheWI.fields."${tName}"
		String changedDate = "${cacheWI.fields.'System.ChangedDate'}"
		def pVal = prevWI.fields."${tName}"
		flag = "${pVal}" == "${cVal}"
		if (!flag) {
			log.info("ADO field change cached:  key: ${key}-${tName}, date: ${changedDate}.")
			cacheManagementService.saveToCache([changeDate: changedDate, value: cVal], "${key}-${tName}", 'changedField')
		}
		return flag
	}
	
	private def getFieldData(def qmItemData, def id, def field, def memberMap, def cacheWI, def map, def resultMap = null, def testCase = null, def prevWI = null, def exData = null) {
		String handlerName = "${field.source}"
		String qmHandlerName = "Qm${handlerName.substring(0,1).toUpperCase()}${handlerName.substring(1)}"
		String fValue = ""
		if (this.fieldMap[qmHandlerName] != null) {
			def data = [itemData: qmItemData, id: id, memberMap: memberMap, fieldMap: field, cacheWI: cacheWI, prevWI: prevWI, itemMap: map, resultMap: resultMap, testCase: testCase, exData: exData]
			if (testCase != null) {
				data['testCase'] = testCase
			}
			if (resultMap != null) {
				data['resultMap'] = resultMap
			}
			def fieldData = this.fieldMap[qmHandlerName].execute(data)
			return fieldData
		} else if (this.fieldMap["${handlerName}"] != null) {
			def data = [itemData: qmItemData, id: id, memberMap: memberMap, fieldMap: field, cacheWI: cacheWI, prevWI: prevWI, itemMap: map, resultMap: resultMap, testCase: testCase, exData: exData]
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
	
	/**
	 * Method for creating link data. A aspect will ensure link data is cached.
	 * 
	 * @param id - element id
	 * @param timeStamp - Link data creation timestamp
	 * @param testItem - CLM test element
	 * @return link data
	 */
	//@Cache(elementType = LinkInfo)
	public List<LinkInfo> getAllLinks(String id, Date timeStamp, testItem) {
		List<LinkInfo> links = new ArrayList<LinkInfo>()
		String itype = "${testItem.name()}"
		String otype = itemMap[itype]
		String wid = "${testItem.webId.text()}-${otype}"
		testItem.requirement.each { req ->
			String rid = "${req.@href}"
			LinkInfo info = new LinkInfo(type: 'requirement', itemIdCurrent: wid, itemIdRelated: rid, moduleCurrent: 'QM', moduleRelated: 'RM')
			links.add(info)
		}
		testItem.relatedChangeRequest.each { wi ->
			String rid = "${wi.@href}"
			LinkInfo info = new LinkInfo(type: 'relatedChangeRequest', itemIdCurrent: wid, itemIdRelated: rid, moduleCurrent: 'QM', moduleRelated: 'CCM')
			links.add(info)
		}
		return links
		
	}


	private def getTestMaps(qmItemData) {
		String type = "${qmItemData.name()}"
		def maps = testMappingManagementService.mappingData.findAll { amap -> 
			"${amap.source}" == "${type}"
		}
		return maps
	}
	
	

}

class LinkHolder implements CacheWData {
	
	String data 

	@Override
	public void doData(Object result) {
		data = new JsonBuilder(result).toPrettyString()
		
	}

	@Override
	public Object dataValue() {
		List<LinkInfo> info = []
		def infoData = new JsonSlurper().parseText(data)
		infoData.each { map ->
			def aInfo = LinkInfo.newInstance(map)
			info.add(info)
		}
		return info;
	}
	
}
