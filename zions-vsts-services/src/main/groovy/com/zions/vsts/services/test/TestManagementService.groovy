package com.zions.vsts.services.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import com.zions.common.services.cache.ICacheManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient;
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType

/**
 * Handles all ADO Test Manager specific requests.
 * 
 * <p><b>Design:</b></p>
 * <img src="TestManagementService.png"/>
 * 
 * @author z091182
 * 
 * @startuml
 * annotation Component
 * annotation Autowired
 * class TestManagementService [[java:com.zions.vsts.services.test.TestManagementService]] {
 * 	-GenericRestClient genericRestClient
 * 	-ProjectManagementService projectManagmentService
 * 	~ICacheManagementService cacheManagementService
 * 	+TestManagementService()
 * 	+def sendResultChanges(String collection, String project, def executionResult, String id)
 * 	+def batchPlanChanges(String collection, String tfsProject, def changeList, def idMap)
 * 	+def sendPlanChanges(String collection, String tfsProject, def change, String id)
 * 	+def cleanupTestItems(String collection, String project, String teamArea)
 * 	+def getTestRuns(def project)
 * 	+def setParent(def parent, def children, def map)
 * 	+def ensureTestRun(String collection, String project, def planData)
 * 	-String getTestChangeType(def change)
 * 	-def getTestWorkItems(String collection, String project, String teamArea)
 * 	-def getWorkItem(String url)
 * 	-def associateCaseToPlan(def planData, def tcids)
 * 	-def addTestCase(String suiteUrl, String tcIds)
 * 	-def filterTestCaseIds(String url, def ids)
 * 	-def getSuiteTestCase(String url)
 * 	-def associateCaseToSuite(def suiteData, def tcids)
 * 	-def getPlanSuites(def planData)
 * 	-boolean hasSuite(def planData, String id)
 * 	-String getTargetName(String name, def map)
 * 	-def createRunData(String collection, String project, def planData)
 * 	-def getTestPoints(String collection, String project, def planData)
 * }
 * TestManagementService ..> Component
 * TestManagementService ..> Autowired
 * TestManagementService --> IGenericRestClient: @Autowired genericRestClient
 * TestManagementService --> ICacheManagementService : @Autowired cacheManagementService
 * TestManagementService -> ProjectManagementService: @Autowired projectManagmentService
 * @enduml
 * 
 */
@Component
public class TestManagementService {
	@Autowired(required=true)
	private GenericRestClient genericRestClient;
	
	@Autowired(required=true)
	private ProjectManagementService projectManagmentService;

	@Autowired
	ICacheManagementService cacheManagementService
	
	public TestManagementService() {
		
	}
	
	/**
	 * Send changes for test case test results.
	 * 
	 * @param collection ADO organization
	 * @param project ADO project name
	 * @param executionResult data for result data
	 * @param id cache identity
	 * @return ADO test result representation 
	 */
	public def sendResultChanges(String collection, String project, def inexecutionResult, String id) {
		def executionResult = inexecutionResult.Result
		String method = "${executionResult.method}"
		executionResult.remove('method')
		def result = null
		String nuri = "${genericRestClient.getTfsUrl()}${executionResult.uri}"
		executionResult.uri = nuri
		String jsonBody = new JsonBuilder(executionResult).toPrettyString()
		if (method == 'post') {
			result = genericRestClient.post(executionResult)
		} else if (method == 'patch') {
			result = genericRestClient.patch(executionResult)
		}
//		if (result != null) {
//			cacheManagementService.saveToCache(result, id, ICacheManagementService.RESULT_DATA)
//		}
		return result
	}
	
	public def batchPlanChanges(String collection, String tfsProject, def changeList, def idMap) {
		int count = 0
		changeList.each { change ->
			String nuri = "${genericRestClient.getTfsUrl()}${change.uri}"
			change.uri = nuri
			String method = "${change.method}"
			
			((Map) change).remove('method')
			def result = null
			String dataType = getTestChangeType(change)
			if (method == 'post') {
				result = genericRestClient.post(change)
			} else if (method == 'patch') {
				result = genericRestClient.patch(change)
			}
			if (result != null) {
				cacheManagementService.saveToCache(result, idMap[count], dataType)
			}
			count++
		}
	}
	
	public def sendPlanChanges(String collection, String tfsProject, def change, String id) {
		String nuri = "${genericRestClient.getTfsUrl()}${change.uri}"
		change.uri = nuri
		String method = "${change.method}"
		
		((Map) change).remove('method')
		def result = null
		String dataType = getTestChangeType(change)
		if (method == 'post') {
			result = genericRestClient.post(change)
		} else if (method == 'patch') {
			result = genericRestClient.patch(change)
		}
		if (result != null) {
			cacheManagementService.saveToCache(result, id, dataType)
		}
		return result
	}
	
	private def getResultsTestcaseMap(def url) {
		def result = genericRestClient.get(
			uri: url,
			contentType: ContentType.JSON,
			query: [destroy: true, 'api-version': '5.0-preview.3']
			)
		def tcMap = [:]
		result.'value'.each { aresult ->
			tcMap["${aresult.testCase.id}"] = aresult
		}
		return tcMap

	}

	private String getTestChangeType(def change) {
		String type = ICacheManagementService.PLAN_DATA
		String uri = "${change.uri}"
		if (uri.indexOf('suites') > -1) {
			type = ICacheManagementService.SUITE_DATA
		}
		return type
	}
	
//	def getQueryHierarchy(def project) {
//		def collection = ""
//		def projectInfo = projectManagmentService.getProject(collection, project)
//		def result = genericRestClient.get(
//			contentType: ContentType.JSON,
//			uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectInfo.id}/_api/_TestQueries/GetQueryHierarchy",
//			headers: ['Content-Type': 'application/json'],
//			query: [itemTypes: 'ExploratorySession', itemTypes: 'TestResult', itemTypes: 'TestRun']
//			)
//		return result
//	}
	
	public def cleanupTestItems(String collection, String project, String teamArea) {
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def wis = getTestWorkItems(collection, project, teamArea)
		wis.workItems.each { wi ->
			def twi = getWorkItem(wi.url)
			if (twi != null) {
				String type = "${twi.fields.'System.WorkItemType'}"
				if (type == 'Test Plan') {
					String url = "${genericRestClient.getTfsUrl()}/${eproject}/_apis/testplan/plans/${wi.id}"
					genericRestClient.delete(
						uri: url,
						contentType: ContentType.JSON,
						query: [destroy: true, 'api-version': '5.0-preview.1']
						)
				} else if (type == 'Test Case'){
					String url = "${genericRestClient.getTfsUrl()}/${eproject}/_apis/test/testcases/${wi.id}"
					genericRestClient.delete(
						uri: url,
						contentType: ContentType.JSON,
						query: [destroy: true, 'api-version': '5.0-preview.1']
						)
	
				}
			}
		}
//		File cacheL = new File(cacheLocation)
//		cacheL.deleteDir()
	}
	
	private def getTestWorkItems(String collection, String project, String teamArea) {
		def eproject = URLEncoder.encode(project, 'utf-8')
		eproject = eproject.replace('+', '%20')
		def query = [query: "Select [System.Id], [System.Title] From WorkItems Where ([System.WorkItemType] = 'Test Plan'  OR [System.WorkItemType] = 'Test Case') AND [System.AreaPath] = '${teamArea}'"]
		String body = new JsonBuilder(query).toPrettyString()
		def result = genericRestClient.post(
			requestContentType: ContentType.JSON,
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/wit/wiql",
			body: body,
			//headers: [Accept: 'application/json'],
			query: ['api-version': '5.0-preview.2']
			)
		return result
	}
	
	private def getWorkItem(String url) {
		def result = genericRestClient.get(
			uri: url,
			contentType: ContentType.JSON,
			query: [destroy: true, 'api-version': '5.0-preview.3']
			)
		return result
	}
	
	public def getTestRuns(def project) {
		def collection = ""
		def projectInfo = projectManagmentService.getProject(collection, project)
		//def queryHierarchy = getQueryHierarchy(project)
//		def query = new JsonBuilder( queryHierarchy.queries[0] ).toString()
//		
//		def queryJson = [queryJson: query]
//		def body = new JsonBuilder( queryJson ).toString()
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			//requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectInfo.id}/_apis/test/runs",
			headers: ['Content-Type': 'application/json'],
			query: ['api-version':'5.0-preview.2', includeRunDetails: true]
			)

		return result;

	}
	
	public def setParent(def parent, def children, def map) {
		String pname = "${parent.name()}"
		String ptname = getTargetName(pname, map)
		String pid = "${parent.webId.text()}-${ptname}"
		String type = ICacheManagementService.PLAN_DATA
		if (pname == 'testsuite') type = ICacheManagementService.SUITE_DATA
		def parentData = cacheManagementService.getFromCache(pid, type)
		if (parentData != null) {
			def tcIds = []
			int tot = children.size()
			int count = 0
			children.each { child -> 
				String cname = "${child.name()}"
				
				String ctname = getTargetName(cname, map)
				
				
				String cid = "${child.webId.text()}-${ctname}"
				def childData = cacheManagementService.getFromCache(cid, ICacheManagementService.WI_DATA)
				if (childData != null) {
					tcIds.add("${childData.id}")
				}
				if (tcIds.size() == 5 || (count+1==tot && tcIds.size() > 0)) {
					if ("${pname}" == 'testplan') {
						associateCaseToPlan(parentData, tcIds)
					} else if ("${pname}" == 'testsuite') {
						associateCaseToSuite(parentData, tcIds)
					}
					tcIds = []
				}
				count++
			}
			
		}
		
	}
	
	
	private def associateCaseToPlan(def planData, def tcids) {
		String suiteUrl = "${planData.rootSuite.url}"
		//def ids = filterTestCaseIds(suiteUrl, tcids)
		if (tcids.size()>0) {
			String tcIds = tcids.join(',')
			addTestCase(suiteUrl, tcIds)
		}
	}
	private def addTestCase(String suiteUrl, String tcIds) {
		String tcUrl = "${suiteUrl}/testcases/${tcIds}"
		
		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			//requestContentType: ContentType.JSON,
			uri: tcUrl,
			query: ['api-version':'5.0-preview.3']
			)
		return result
	}
	private def filterTestCaseIds(String url, def ids) {
		def tcs = getSuiteTestCase(url)
		def otcs = ids.findAll { id ->
			boolean excludesId = true
			tcs.'value'.each { tc ->
				if ("${tc.id}" == "${id}") {
					excludesId = false
					return
				}
			}
			return excludesId
		}
		return otcs
	}
	
	private def getSuiteTestCase(String url) {
		String tcUrl = "${url}/testcases"
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			//requestContentType: ContentType.JSON,
			uri: url,
			query: ['api-version':'5.0-preview.3']
			)
		return result

	}
	
	private def associateCaseToSuite(def suiteData, def tcids) {
		String suiteUrl = "${suiteData.url}"
		def ids = filterTestCaseIds(suiteUrl, tcids)
		if (ids.size()>0) {
			String tcIds = ids.join(',')
			addTestCase(suiteUrl, tcIds)
		}
	}
	
	private def getPlanSuites(def planData) {
		String url = "${planData._links.self}/suites"
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			//requestContentType: ContentType.JSON,
			uri: url,
			query: ['api-version':'5.0-preview.3']
			)
		return result
	}
	
	private boolean hasSuite(def planData, String id) {
		def suites = getPlanSuites(planData)
		Collection suite = suites.'value'.findAll { asuite ->
			"${asuite.id}" == "${id}"
		}
		return suite.size()>0
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
	

	public def ensureTestRun(String collection, String project, def planData) {
		String pid = "${planData.webId.text()}-Test Plan"
		def runData = cacheManagementService.getFromCache(pid, ICacheManagementService.RUN_DATA)
		
		if (runData == null) {
			def parentData = cacheManagementService.getFromCache(pid, ICacheManagementService.PLAN_DATA)
		
			runData = createRunData(collection, project, parentData)
			if (runData != null) {
				cacheManagementService.saveToCache(runData, pid, ICacheManagementService.RUN_DATA)
			}
		}
		def resultTestCaseMap = getResultsTestcaseMap("${runData.url}/results")
		return resultTestCaseMap
	}
	
	private def createRunData(String collection, String project, def planData ) {
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def testpoints = getTestPoints(collection, project, planData)
		def data = [name: "${planData.name} Run", plan: [id: planData.id], pointIds:testpoints]
		String body = new JsonBuilder( data ).toString()
		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/test/runs",
			body: body,
			query: ['api-version':'5.0-preview.2']
			)
		return result
	}

	private def getTestPoints(String collection, String project, def planData ) {
		def retVal = []
		String url = "${planData.rootSuite.url}/points"
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			//requestContentType: ContentType.JSON,
			uri: url,
			query: ['api-version':'5.0-preview.2']
			)
		result.'value'.each { point ->
			retVal.add(point.id)
			
		}
		return retVal
	}
	
	private def encodeFile( Object data ) throws UnsupportedEncodingException {
		if ( data instanceof File ) {
			def entity = new org.apache.http.entity.FileEntity( (File) data, "application/json" );
			entity.setContentType( "application/json" );
			return entity
		} else {
			throw new IllegalArgumentException(
				"Don't know how to encode ${data.class.name} as a zip file" );
		}
	}


}
