package com.zions.vsts.services.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.restart.ICheckpointManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.work.WorkManagementService
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType

/**
 * Handles all ADO Test Manager specific requests.
 *
 * <p><b>Design:</b></p>
 * <img src="TestManagementService.svg"/>
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
 *  +def ensureResultAttachment(String collection, String tfsProject, def rfiles, def testcase, def resultMap)
 *  -boolean hasAttachment(String url, String filename)
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
@Slf4j
public class TestManagementService {
	@Autowired(required=true)
	private IGenericRestClient genericRestClient;
	
	@Autowired(required=true)
	private ProjectManagementService projectManagmentService;

	@Autowired
	ICacheManagementService cacheManagementService
	
	@Autowired(required=false)
	ICheckpointManagementService checkpointManagementService
	
	@Autowired
	WorkManagementService workManagementService
	
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
	public def sendResultChanges(String collection, String project, def executionResult, String id) {
		//def executionResult = inexecutionResult.Result
		//String json = new JsonBuilder(executionResult).toPrettyString()
		//log.info("Processing execution result: ${json}")
		String method = "${executionResult.method}"
		executionResult.remove('method')
		def result = null
		String nuri = "${genericRestClient.getTfsUrl()}${executionResult.uri}"
		executionResult.uri = nuri
		// For test data.
		//String jsonBody = new JsonBuilder(executionResult).toPrettyString()
		if (method == 'post') {
			result = genericRestClient.post(executionResult)
		} else if (method == 'patch') {
			result = genericRestClient.patch(executionResult)
		}
		if (result != null && result.count == 1) {
			String ourl = "${nuri}/${result.value[0].id}"
			def eResult = getResult(ourl)
//			String rjson = new JsonBuilder(eResult).toPrettyString()
//			log.info("Finished execution result: ${rjson}")
			if (eResult) {
				result = eResult
				cacheManagementService.saveToCache(eResult, id, ICacheManagementService.RESULT_DATA)
			}
		}
		if (result == null) {
			log.error("Failed to save result:  ${id}")
			//checkpointManagementService.addLogentry("Unable to save test result with id:  ${id}")
		}
		
		return result
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
	public def sendResultChangesMulti(String collection, String project, def executionResult, def ids) {
		//def executionResult = inexecutionResult.Result
		String method = "${executionResult.method}"
		executionResult.remove('method')
		def result = null
		String nuri = "${genericRestClient.getTfsUrl()}${executionResult.uri}"
		executionResult.uri = nuri
		// For test data.
		//String jsonBody = new JsonBuilder(executionResult).toPrettyString()
		if (method == 'post') {
			result = genericRestClient.post(executionResult)
		} else if (method == 'patch') {
			result = genericRestClient.patch(executionResult)
		}
//		if (result != null) {
//			int i = 0
//			result.value.each { r ->
//				String uri = "${nuri}/${r.id}"
//				def eResult = getResult(uri)
//				String key = "${r.id}"
//				String id = ids[key]
//				cacheManagementService.saveToCache(eResult, id, ICacheManagementService.RESULT_DATA)
//			}
//		}
//		if (result == null) {
//			checkpointManagementService.addLogentry("Unable to save test result with id:  ${id}")
//		}
		return result
	}

	public def resetToActive(def testcase, def resultMap) {
		String key = "${testcase.id}"
		def result = resultMap[key]
		if (result) {
			String projectId = "${result.project.id}"
			String planId = "${result.testPlan.id}"
			String pointId = "${result.testPoint.id}"
			def bodyData = [planId: planId, testPointIds: [pointId]]
			def rresult = genericRestClient.rateLimitPost(
					uri: "${genericRestClient.getTfsUrl()}/${projectId}/_api/_testManagement/ResetTestPoints",
					contentType: ContentType.JSON,
					requestContentType: ContentType.JSON,
					body: bodyData,
					query: ['__v': '5']
				)
			return rresult
		}
		return null
	}
	
	public def resetTestPointsToActive(def suite, def points) {
		String key = "${suite.webId.text()}-Test Suite"
		def adoSuite = cacheManagementService.getFromCache(key, ICacheManagementService.SUITE_DATA)
		if (adoSuite) {
			String spoints = points.join(',')
			String projectId = "${adoSuite.project.id}"
			String suiteId = "${adoSuite.id}"
			String planId = "${adoSuite.plan.id}"
			def body = [ 'resetToActive': true ]
			def rresult = genericRestClient.patch(
					uri: "${genericRestClient.getTfsUrl()}/${projectId}/_apis/test/Plans/${planId}/Suites/${suiteId}/points/${spoints}",
					contentType: ContentType.JSON,
					requestContentType: ContentType.JSON,
					body: body,
					query: ['api-version': '5.0']
				)
			return rresult
		}
		return null
	}
	
	public String getTestPoint(def testcase, def resultMap) {
		String key = "${testcase.id}"
		def result = resultMap[key]
		if (result) {
			String projectId = "${result.project.id}"
			String planId = "${result.testPlan.id}"
			String pointId = "${result.testPoint.id}"
			return pointId
		}
		return null

	}
	
	/**
	 * Batch a set of plan item changes to ADO.
	 *
	 * @param collection - ADO organization
	 * @param tfsProject - ADO project name
	 * @param changeList - Set of ADO request objects
	 * @param idMap - Map of RQM to ADO ids
	 * @return
	 */
//	public def batchPlanChanges(String collection, String tfsProject, def changeList, def idMap) {
//		int count = 0
//		changeList.each { change ->
//			String nuri = "${genericRestClient.getTfsUrl()}${change.uri}"
//			change.uri = nuri
//			String method = "${change.method}"
//
//			((Map) change).remove('method')
//			def result = null
//			String dataType = getTestChangeType(change)
//			if (method == 'post') {
//				result = genericRestClient.post(change)
//			} else if (method == 'patch') {
//				result = genericRestClient.patch(change)
//			}
//			if (result != null) {
//				cacheManagementService.saveToCache(result, idMap[count], dataType)
//			}
//			count++
//		}
//	}
	
	/**
	 * Send a single plan item changesp
	 *
	 * <p><b>Flow</b></p>
	 * <img src="TestManagmentService_sendPlanChanges.svg"/>
	 *
	 * @param collection - ADO organization
	 * @param tfsProject - ADO project
	 * @param change - ADO plan item request data
	 * @param id - cache item ID
	 * @return ADO request result.
	 *
	 * @startuml TestManagmentService_sendPlanChanges.svg
	 * participant "TranslateRQMToMTM:caller" as caller
	 * participant "TestManagementService:this" as this
	 * participant "Map:change" as change
	 * participant "IGenericRestClient:genericRestClient" as genericRestClient
	 * participant "ICacheManagementService:cacheManagmentService" as cacheManagementService
	 * caller -> this: sendPlanChanges(collection, adoProject, change, cacheId)
	 * this -> genericRestClient: tfsUrl
	 * this -> this: build request url
	 * this -> change: get request method (post or patch)
	 * this -> change: remove method
	 * alt method == 'post'
	 * 	this -> genericRestClient: post(modified change) : response
	 * else method == 'patch'
	 * 	this -> genericRestClient: post(modified change) : response
	 * end
	 * alt response != null
	 * 	this -> cacheManagementService: saveToCache(result, cacheId, cacheType)
	 * end
	 * @enduml
	 *
	 */
	public def sendPlanChanges(String collection, String tfsProject, def change, String id) {
		// Test data
//		File changeData = new File('change.json')
//		def os = changeData.newDataOutputStream()
//		os << new JsonBuilder(change).toPrettyString()
//		os.close()
		String nuri = "${genericRestClient.getTfsUrl()}${change.uri}"
		change.uri = nuri
		String method = "${change.method}"
		
		((Map) change).remove('method')
		def result = null
		String dataType = getTestChangeType(change)
		String body = new JsonBuilder(change.body).toPrettyString()
		change.body = body
		if (method == 'post') {
			result = genericRestClient.post(change)
		} else if (method == 'patch') {
			result = genericRestClient.patch(change)
		}
		if (result != null) {
			if (result.'value') {
				result = result.'value'[0]
			}
			cacheManagementService.saveToCache(result, id, dataType)
			String wid = "${result.id}"
			
			def wi = workManagementService.getWorkItem(collection, tfsProject, wid)
			if (wi) {
				cacheManagementService.saveToCache(wi, "${id} WI", ICacheManagementService.WI_DATA)
			}
		} else {
			log.error("Unable to save data for ${dataType} with id:  ${id}")
			if (checkpointManagementService) {
				checkpointManagementService.addLogentry("Unable to save data for ${dataType} with id:  ${id}")
			}
		}
		return result
	}
	
	public def ensureResultAttachments(String collection, String tfsProject, def rfiles, def testcase, def resultMap) {
		String tcWebId = "${testcase.webId.text()}-Test Case"
		def adoTestCase = cacheManagementService.getFromCache(tcWebId, ICacheManagementService.WI_DATA)
		def result = resultMap["${adoTestCase.id}"]
		def attachmentUrl = "${result.url}/attachments"
		rfiles.each { afile ->
			File attFile = afile.file
			String name = attFile.name
			if (!hasAttachment(attachmentUrl, name)) {
				String stream = attFile.bytes.encodeBase64()
				String comment = "${afile.comment}"
				def body = [attachmentType: 'GeneralAttachment', fileName: name, comment: comment, stream: stream]
				def rresult = genericRestClient.post(
					uri: attachmentUrl,
					contentType: ContentType.JSON,
					requestContentType: ContentType.JSON,
					body: body,
					query: [destroy: true, 'api-version': '5.0-preview.1']
					)
		
			}
		}
	}
	
	public def cleanupTestItems(String collection, String project, String teamArea, String query = null) {
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def wis = getTestWorkItems(collection, project, teamArea, query)
		while (true) {
			wis.workItems.each { wi ->
				def twi = getWorkItem(wi.url)
				if (twi != null) {
					String type = "${twi.fields.'System.WorkItemType'}"
					if (type == 'Test Plan') {
						String url = "${genericRestClient.getTfsUrl()}/${eproject}/_apis/testplan/plans/${wi.id}"
						def result = genericRestClient.delete(
							uri: url,
							contentType: ContentType.JSON,
							query: [destroy: true, 'api-version': '5.0-preview.1']
							)
					} else if (type == 'Test Case'){
						String url = "${genericRestClient.getTfsUrl()}/${eproject}/_apis/testplan/testcases/${wi.id}"
						def result = genericRestClient.delete(
							uri: url,
							contentType: ContentType.JSON,
							query: [destroy: true, 'api-version': '5.0-preview.1']
							)
					}
				}
			}
			wis = getTestWorkItems(collection, project, teamArea, query)
			if (!wis || wis.workItems.size() == 0) break;
		}
		cacheManagementService.clear()
//		File cacheL = new File(cacheLocation)
//		cacheL.deleteDir()
	}
	
	String getPlanId(def planData) {
		String className = planData.getClass().simpleName
		if (className == 'TestPlan') {
			return "${planData.id}"
		}
		return "${planData.webId.text()}-Test Plan"
	}
	
	String getSuiteId(def suiteData) {
		String className = suiteData.getClass().simpleName
		if (className == 'TestSuite') {
			return "${suiteData.id}"
		}
		return "${suiteData.webId.text()}-Test Suite"
	}

	def getPlan(String collection, String project, String planName) {
		
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/test/plans",
			query: ['api-version':'5.0']
			)
		def outPlan = null
		if (result) {
			result.'value'.each { plan ->
				String name = "${plan.name}"
				if (name == planName) {
					outPlan = plan
					return outPlan
				}
			}
		}
		return outPlan
	}
	
	//getPlan by test plan Id
	def getPlan(String collection, String project, Integer Id) {
		
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			//verify API call for testplan ID
			//should work//https://dev.azure.com/eto-dev/ALMOpsTest/_apis/test/plans/458154
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/test/plans/${Id}",
			query: ['api-version':'5.0']
			)
		
		return result
		
	}
	
	def getTestSuite(String collection, String project, String Id) {
		
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			//verify API call for testplan ID
			//should work//https://dev.azure.com/eto-dev/ALMOpsTest/_apis/test/plans/458154
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/testplan/Plans/${Id}/suites?api-version=5.1-preview.1",
			query: ['api-version':'5.0']
			)
		
		return result
		
	}
	
	def getTestPointFromSuite(String collection, String project, String testPlanId, String testSuiteId) {
		
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			//verify API call for testplan ID
			//should work//https://dev.azure.com/eto-dev/ALMOpsTest/_apis/test/plans/458154
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/test/Plans/${testPlanId}/Suites/${testSuiteId}/points?api-version=6.0",
			//uri: https://dev.azure.com/zionseto/Sandbox/_apis/test/Plans/1695461/Suites/1696010/points?api-version=6.0
			query: ['api-version':'6.0']
			)
		
		return result
		
	}
	
	def getTestPointDataUsingPoint(String collection, String project, String testPlanId, String testSuiteId, String testPointId) {
		
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			//verify API call for testplan ID
			//should work//https://dev.azure.com/eto-dev/ALMOpsTest/_apis/test/plans/458154
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/test/Plans/${testPlanId}/Suites/${testSuiteId}/points/${testPointId}?api-version=6.0",
			//uri: https://dev.azure.com/zionseto/Sandbox/_apis/test/Plans/1695461/Suites/1696010/points?api-version=6.0
			query: ['api-version':'6.0']
			)
		
		return result
		
	}
	
	

	
	def getSuite(def plan, String suiteName) {
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${plan.url}/suites",
			query: ['api-version':'5.0']
			)
		def outSuite = null
		if (result) {
			result.'value'.each { suite ->
				String name = "${suite.name}"
				if (name == suiteName) {
					outSuite = plan
					return outSuite
				}
			}
		}
		return outSuite
	}
	
	
	def getSuiteMap(def plan) {
		def suiteMap = [:]
		def result = genericRestClient.get(
			uri: "${plan.url}/suites",
			contentType: ContentType.JSON,
			query: ['api-version': '5.0']
			)
		result.'value'.each { suite ->
			suiteMap["${suite.name}"] = suite
		}
		return suiteMap
	}

	String getTestCaseId(def testcaseData) {
		String className = testcaseData.getClass().simpleName
		if (className == 'TestCase') {
			return "${testcaseData.id}"
		}
		return "${testcaseData.webId.text()}-Test Case"
	}
	
	private def deleteRun(def runData) {
		def r = genericRestClient.delete(
				uri: "${runData.url}",
				query: ['api-version': '5.1']
			)
		return r
	}

	public def ensureTestRun(String collection, String project, def planData, boolean refresh = false) {
		String pid = getPlanId(planData)
		def runData = cacheManagementService.getFromCache(pid, ICacheManagementService.RUN_DATA)
		
		if (refresh && runData) {
			deleteRun(runData)
			runData = null
			//cacheManagementService.deleteFromCache(pid, ICacheManagementService.RUN_DATA)
		}
		
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
	
	public def cloneTestPlan(collection, destPlanName, testplanId, srcProjectName, destProjectName) {
	//call getTestPlan to get source the test plan
		//executionResult.uri replace with clone
		def eproject = URLEncoder.encode(srcProjectName, 'utf-8')
		eproject = eproject.replace('+', '%20')
		def uri = "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/test/Plans/${testplanId}/cloneoperation?api-version=5.0-preview.2"
		def body = ['destinationTestPlan': [ 'name': destPlanName, 'Project': [ 'Name': destProjectName ]], 'options': [ 'copyAncestorHierarchy': true, 'copyAllSuites': true, 'overrideParameters': [ 'System.AreaPath': destProjectName, 'System.IterationPath': destProjectName ]], 'suiteIds': [ 2 ]]
		String sbody = new JsonBuilder(body).toPrettyString()
		//put stop here json builder to prettystring look at what sbody looks like as formatted json
		//should have same format as body in successful talend execution
		def result = genericRestClient.rateLimitPost(
			requestContentType: ContentType.JSON,
			contentType: ContentType.JSON,
			uri: uri,
			body: sbody,
			//headers: [Accept: 'application/json'],
			query: ['api-version': '5.1-preview.1' ]
			)
		return result
	}
	
	public def updateTestPoint(collection, project, testplanId, testsuiteId, testpointId, outcome, state, runBy, tester) {

			def eproject = URLEncoder.encode(project, 'utf-8')
			eproject = eproject.replace('+', '%20')
			
			def uri = "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/test/Plans/${testplanId}/Suites/${testsuiteId}/points/${testpointId}?api-version=6.0&bypassRules=True&suppressNotifications=true"
			
			//def body = ['outcome': outcome, 'state': state, 'runBy': [ 'displayName': runBy]]
			def body = ['outcome': outcome, 'state': state, 'runBy': [ 'displayName': runBy], 'tester': [ 'id': tester]]
			
			
			String sbody = new JsonBuilder(body).toPrettyString()
			//put stop here json builder to prettystring look at what sbody looks like as formatted json
			//should have same format as body in successful talend execution
			def result = genericRestClient.patch(
				requestContentType: ContentType.JSON,
				contentType: ContentType.JSON,
				uri: uri,
				body: sbody,
				//headers: [Accept: 'application/json'],
				query: ['api-version': '5.1-preview.1' ]
				)
			return result
		}
		
		public def updateTestPointTester(collection, project, testplanId, testsuiteId, testpointId, tester) {
			
				def eproject = URLEncoder.encode(project, 'utf-8')
				eproject = eproject.replace('+', '%20')
				
				def uri = "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/test/Plans/${testplanId}/Suites/${testsuiteId}/points/${testpointId}?api-version=6.0&bypassRules=True&suppressNotifications=true"
				
				//def body = ['outcome': outcome, 'state': state, 'runBy': [ 'displayName': runBy]]
				def body = ['tester': [ 'displayName': tester]]
				
				
				String sbody = new JsonBuilder(body).toPrettyString()
				//put stop here json builder to prettystring look at what sbody looks like as formatted json
				//should have same format as body in successful talend execution
				def result = genericRestClient.patch(
					requestContentType: ContentType.JSON,
					contentType: ContentType.JSON,
					uri: uri,
					body: sbody,
					//headers: [Accept: 'application/json'],
					query: ['api-version': '5.1-preview.1' ]
					)
				return result
			}
	
		public def createTestRun(collection, project, testplanId, testpointIds, comment, owner, name, state, startedDate, completedDate) {
						
			def eproject = URLEncoder.encode(project, 'utf-8')
			eproject = eproject.replace('+', '%20')
			
			def uri = "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/test/runs?api-version=6.0&bypassRules=True&suppressNotifications=true"
			//def body = ['name': name, 'state': state, 'starteDate': startedDate, 'completedDate': completedDate, 'owner': [ 'displayName': owner], , 'pointIds': [ testpointId ]]
			def body = ['name': name, 'state': state, 'comment': comment, 'starteDate': startedDate, 'completedDate': completedDate, 'owner': [ 'displayName': owner], 'plan': [ 'id': testplanId], 'pointIds':  testpointIds ]
			
			
			String sbody = new JsonBuilder(body).toPrettyString()
			//put stop here json builder to prettystring look at what sbody looks like as formatted json
			//should have same format as body in successful talend execution
			def result = genericRestClient.rateLimitPost(
			  
				requestContentType: ContentType.JSON,
				contentType: ContentType.JSON,
				uri: uri,
				body: sbody,
				//headers: [Accept: 'application/json'],
				query: ['api-version': '5.1-preview.1' ]
				)
			return result
			}
				
				
	
	
			public def updateTestResult(collection, project, runId, resultId, priority, outcome, testCaseTitle, state, startedDate, completedDate, lastupdatedDate, createdDate, configuration, comment, owner, runBy) {
				
				def eproject = URLEncoder.encode(project, 'utf-8')
				eproject = eproject.replace('+', '%20')
				
				def uri = "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/test/Runs/${runId}/results?api-version=6.0&bypassRules=True&suppressNotifications=true"
				//def body = ['destinationTestPlan': [ 'name': destPlanName, 'Project': [ 'Name': destProjectName ]], 'options': [ 'copyAncestorHierarchy': true, 'copyAllSuites': true, 'overrideParameters': [ 'System.AreaPath': destProjectName, 'System.IterationPath': destProjectName ]], 'suiteIds': [ 2 ]]
				//def body = ['name': name, 'state': state, 'starteDate': startedDate, 'completedDate': completedDate, 'owner': [ 'displayName': owner], , 'pointIds': [ testpointId ]]
				def body = [['priority': priority, 'id': resultId, 'outcome': outcome, 'testCaseTitle': testCaseTitle, 'state': state, 'startedDate': startedDate,
				'completedDate': completedDate, 'lastUpdatedDate': lastupdatedDate, 'createdDate': createdDate, 'comment': comment, 'owner': [ 'displayName': owner],
				'runBy': [ 'displayName': runBy], 'configuration': [ 'id': configuration],]]
				
				String sbody = new JsonBuilder(body).toPrettyString()
				//put stop here json builder to prettystring look at what sbody looks like as formatted json
				//should have same format as body in successful talend execution
				//def result = genericRestClient.rateLimitPost(
				def result = genericRestClient.patch(
					requestContentType: ContentType.JSON,
					contentType: ContentType.JSON,
					uri: uri,
					body: sbody,
					//headers: [Accept: 'application/json'],
					query: ['api-version': '5.1-preview.1' ]
					)
				return result
				}
	
	
	
	
	public def ensureTestRunForTestCaseAndPlan(String collection, String project, def planData, def testcaseData, def testCasePointsMap = null) {
		String pid = getPlanId(planData)
		String tcid = getTestCaseId(testcaseData)
		String key = "${tcid}_${pid}"
		def runData = cacheManagementService.getFromCache(key, ICacheManagementService.RUN_DATA)
		
		if (runData == null) {
			def adoPlanData = cacheManagementService.getFromCache(pid, ICacheManagementService.PLAN_DATA)
			def adoTestCaseData = cacheManagementService.getFromCache(tcid, ICacheManagementService.WI_DATA)
			
			runData = createRunDataForTestCase(collection, project, adoPlanData, adoTestCaseData, testCasePointsMap)
			if (runData != null) {
				cacheManagementService.saveToCache(runData, key, ICacheManagementService.RUN_DATA)
			}
		}
		def resultTestCaseMap = getResultsTestcaseMap("${runData.url}/results")
		return resultTestCaseMap
	}
	
	public def ensureTestRunForTestCaseAndSuite(String collection, String project, def suiteData, def testcaseData, boolean refresh = false, def testCasePointsMap = null, String exeWebId = null, def runMap = null) {
		String tcid = getTestCaseId(testcaseData)
		String sid = getSuiteId(suiteData)
		String pid = getPlanId(suiteData)
		String key = "${tcid}_${sid}"
		if (exeWebId) {
			def orunData = cacheManagementService.getFromCache(key, ICacheManagementService.RUN_DATA)
			if (orunData) {
				deleteRun(orunData)
			}
			key = exeWebId
		}
		def runData = cacheManagementService.getFromCache(key, ICacheManagementService.RUN_DATA)
		if (refresh && runData) {
			deleteRun(runData)
			//setRunToInprogress(runData)
			runData = null
			
			//cacheManagementService.deleteFromCache(pid, ICacheManagementService.RUN_DATA)
		}
		String pkey = "${tcid}_${pid}"
		def prunData = cacheManagementService.getFromCache(pkey, ICacheManagementService.RUN_DATA)
		if (prunData) {
			deleteRun(prunData)
			cacheManagementService.deleteById(pkey)
		}
		
		if (runData == null) {
			def adoSuiteData = cacheManagementService.getFromCache(sid, ICacheManagementService.SUITE_DATA)
			def adoTestCaseData = cacheManagementService.getFromCache(tcid, ICacheManagementService.WI_DATA)
			if (!adoSuiteData || !adoTestCaseData) return [:]
			if (hasManualRun(adoSuiteData, adoTestCaseData.id, runMap)) return [:]
			runData = createRunDataForTestCase(collection, project, adoSuiteData, adoTestCaseData, null, false, testCasePointsMap)
			if (runData != null) {
				cacheManagementService.saveToCache(runData, key, ICacheManagementService.RUN_DATA)
			}
		}
		def resultTestCaseMap = getResultsTestcaseMap("${runData.url}/results")
		return resultTestCaseMap
	}
	
	boolean hasManualRun(adoSuite, String testCaseId) {
		String manualTitle = "${adoSuite.name} (Manual)".bytes.encodeBase64()
		String key = "${testCaseId}-${manualTitle}"
		def r = cacheManagementService.getFromCache(key, 'manualResult')
		if (r) return true
		return false
	}
	
	def setupManualRuns(adoPlan) {
		String pid = "${adoPlan.project.id}"
		String planId = adoPlan.id
		String url = "${genericRestClient.tfsUrl}/${pid}/_apis/test/runs"
		int skip = 0
		//cacheManagementService.deleteByType('manualResult')
		while (true) {
			def result = genericRestClient.get(
				contentType: ContentType.JSON,
				//requestContentType: ContentType.JSON,
				uri: url,
				headers: ['Content-Type': 'application/json'],
				query: ['api-version':'5.1', planIds: planId, '$top': 200, '$skip': skip]
				)
			skip += 200
			if (!result || !result.'value' || result.'value'.size() == 0) break
			result.'value'.each { arun ->
				if (arun.name && arun.name.endsWith('(Manual)')) {
					String erName = "${arun.name}".bytes.encodeBase64()
					def tcMap = getResultsTestcaseMap("${arun.url}/results")
					tcMap.each { tcId, mresult ->
						String key = "${tcId}-${erName}"
						cacheManagementService.saveToCache(mresult, key, 'manualResult')
					}
				}
			}
		}
		return []

	}
	
	public def cleanSuiteRun(String collection, String project, def suiteData) {
		String pid = getSuiteId(suiteData)
		def runData = cacheManagementService.getFromCache(pid, ICacheManagementService.RUN_DATA)
		if (runData) {
			deleteRun(runData)
			//setRunToInprogress(runData)
			runData = null
			//cacheManagementService.deleteFromCache(pid, ICacheManagementService.RUN_DATA)
		}

	}
	
	def ensureAttachments(def adoresult, def binaries, String rwebId) {
		def attachmentCache = cacheManagementService.getFromCache(rwebId, 'resultAttachments')
		boolean added = false
		binaries.each { binary ->
			if (!resultAttachmentExists(binary, attachmentCache)) {
				def resultAtt = sendResultAttachment(adoresult, binary)
				added = true
			}
		}
		if (added) {
			attachmentCache = getResultAttachments(adoresult)
			cacheManagementService.saveToCache(attachmentCache, rwebId, 'resultAttachments')
		}
		
		def attMap = [:]
		attachmentCache.value.each { a ->
			String fn = "${a.fileName}"
			attMap[fn] = a
		}
		return attMap
	}
	
	def sendResultAttachment(adoResult, binary) {
		ByteArrayInputStream bs = binary.data
		String octet = bs.bytes.encodeBase64()
		def body = [stream: octet, attachmentType: 'GeneralAttachment', comment: binary.comment, fileName: binary.filename]
		String sbody = new JsonBuilder(body).toPrettyString()
		def result = genericRestClient.rateLimitPost(
			requestContentType: ContentType.JSON,
			contentType: ContentType.JSON,
			uri: "${adoResult.url}/attachments",
			body: sbody,
			//headers: [Accept: 'application/json'],
			query: ['api-version': '5.1-preview.1' ]
			)
		return result
	}
	
	def sendManualResultAttachment(adoResult, binary) {
		ByteArrayInputStream bs = binary.data
		String octet = bs.bytes.encodeBase64()
		def body = [stream: octet, attachmentType: 'GeneralAttachment', comment: binary.comment, fileName: binary.filename]
		String sbody = new JsonBuilder(body).toPrettyString()
		def result = genericRestClient.rateLimitPost(
			requestContentType: ContentType.JSON,
			contentType: ContentType.JSON,
			uri: "${adoResult.url}/attachments",
			body: sbody,
			//headers: [Accept: 'application/json'],
			query: ['api-version': '5.1-preview.1', iterationId: 1, actionPath: binary.actionPath ]
			)
		return result
	}

	def getResultAttachments(adoResult) {
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			//requestContentType: ContentType.JSON,
			uri: "${adoResult.url}/attachments",
			headers: ['Content-Type': 'application/json'],
			query: ['api-version':'5.0-preview.1']
			)
		return result
	}
	
	boolean resultAttachmentExists(binary, cacheItem) {
		if (!cacheItem) return false
		String fileName = "${binary.filename}"
		def b = cacheItem.value.find { a ->
			String fn = "${a.fileName}"
			fileName == fn
		}
		if (b) return true
		return false
	}

	public def ensureTestRunForTestSuite(String collection, String project, def suiteData, boolean refresh = false, String ipid = null) {
		
		String pid = getSuiteId(suiteData)
		if (ipid) {
			pid = ipid
		}
		def runData = cacheManagementService.getFromCache(pid, ICacheManagementService.RUN_DATA)
//		if (runData) {
//			setRunToInprogress(runData)
//		}
		if (refresh && runData) {
			deleteRun(runData)
			//setRunToInprogress(runData)
			runData = null
			//cacheManagementService.deleteFromCache(pid, ICacheManagementService.RUN_DATA)
		}

		def adoSuiteData
		if (runData == null) {
			adoSuiteData = cacheManagementService.getFromCache(pid, ICacheManagementService.SUITE_DATA)
			if (!adoSuiteData) return [:]
			runData = createRunDataForTestSuite(collection, project, adoSuiteData, null, false)
			if (runData != null) {
				cacheManagementService.saveToCache(runData, pid, ICacheManagementService.RUN_DATA)
			}
		}
		if (runData == null) return [:]
		def resultTestCaseMap = getResultsTestcaseMap("${runData.url}/results")
		return resultTestCaseMap
	}
	
	def refreshResultCaseForTestSuite(String collection, String project, def suiteData) {
		String pid = getSuiteId(suiteData)
		def runData = cacheManagementService.getFromCache(pid, ICacheManagementService.RUN_DATA)
		if (runData) {
			def resultTestCaseMap = getResultsTestcaseMap("${runData.url}/results")
			return resultTestCaseMap
	
		}
		return null
	}
	
	def setRunToInprogress(runData) {
		def body = [ state: 'InProgress']
		def r = genericRestClient.patch(
			contentType: ContentType.JSON,
			requestContentType: ContentType.JSON,
			uri: "${runData.url}",
			query: ['api-version': '5.1']
		)
		return r
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
	
	public def getTestRunsById(def project, String runId) {
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
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectInfo.id}/_apis/test/runs/${runId}",
			headers: ['Content-Type': 'application/json'],
			query: ['api-version':'5.0-preview.2', includeRunDetails: true]
			)

		return result;

	}
	
	public def getTestPointData(String project, String planId, String suiteId) {
		def collection = ""
		def projectInfo = projectManagmentService.getProject(collection, project)
		
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			//requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project}/_apis/test/Plans/${planId}/Suites/${suiteId}",
			headers: ['Content-Type': 'application/json'],
			query: ['api-version':'5.0-preview.2', includeRunDetails: true]
			)

		return result;

	}
	
	public def getTestRunsStatsById(def project, String runId) {
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
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectInfo.id}/_apis/test/runs/${runId}/Statistics",
			headers: ['Content-Type': 'application/json'],
			query: ['api-version':'5.0-preview.2', includeRunDetails: true]
			)

		return result;

	}
	
	public def getTestRunsByPlanId(collection, def project, String planId) {
		
		def projectInfo = projectManagmentService.getProject(collection, project)
		//def queryHierarchy = getQueryHierarchy(project)
//		def query = new JsonBuilder( queryHierarchy.queries[0] ).toString()
//
//		def queryJson = [queryJson: query]
//		def body = new JsonBuilder( queryJson ).toString()
		
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			//requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectInfo.id}/_apis/test/runs/",
			headers: ['Content-Type': 'application/json'],
			query: ['api-version':'5.0-preview.2', includeRunDetails: true, planId: "${planId}" ]
			)

		return result;

	}
	
	
	public def setFromADOParent(def parentData, def children, def map, boolean update = false) {
//		String pname = "${parent.name()}"
//		String ptname = getTargetName(pname, map)
//		String pid = "${parent.webId.text()}-${ptname}"
//		String type = ICacheManagementService.PLAN_DATA
//		if (pname == 'testsuite') type = ICacheManagementService.SUITE_DATA
//		def parentData = cacheManagementService.getFromCache(pid, type)
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
						//associateCaseToPlan(parentData, tcIds, update)
					} else if ("${pname}" == 'testsuite') {
						associateCaseToSuite(parentData, tcIds, update)
					}
					tcIds = []
				}
				count++
			}
			
		}
		
	}

	
	public def setParent(def parent, def children, def map, String iptname = null) {
		String pname = "${parent.name()}"
		String ptname = iptname
		if (!iptname) {
			ptname = getTargetName(pname, map)
		}
		String pid = "${parent.webId.text()}-${ptname}"
		String type = ICacheManagementService.PLAN_DATA
		if (ptname == 'Test Suite' || ptname == 'Inner Test Suite') type = ICacheManagementService.SUITE_DATA
		def parentData = cacheManagementService.getFromCache(pid, type)
		String suiteUrl = null
		if ("${ptname}" == 'Test Plan' && parentData) {
			suiteUrl = "${parentData.rootSuite.url}"
		} else if (("${ptname}" == 'Test Suite' || "${ptname}" == 'Inner Test Suite') && parentData) {
			suiteUrl = "${parentData.url}"
		}

		if (parentData != null && suiteUrl) {
			def tcIds = []
			int tot = children.size()
			def tcMap = getSuiteTestCaseMap(suiteUrl)
			if (tcMap.size() == tot) return
			children.each { child ->
				String cname = "${child.name()}"
				
				String ctname = getTargetName(cname, map)
				
				
				String cid = "${child.webId.text()}-${ctname}"
				def childData = cacheManagementService.getFromCache(cid, ICacheManagementService.WI_DATA)
				if (childData != null) {
					String id = "${childData.id}"
					tcIds.add(id)
				}
			}
			tcIds = this.filterIds(tcIds, tcMap)
			if (tcIds.size() > 0) {
				def oIds = []
				tcIds.each { id ->
					oIds.add(id)
					if (oIds.size() == 5) {
						associateCaseToSuite(suiteUrl, oIds)
						oIds = []
					}
				}
				if (oIds.size() > 0) {
					associateCaseToSuite(suiteUrl, oIds)
				}
			}

		}
		
	}

	
	private boolean hasAttachment(String url, String filename) {
		
		def result = genericRestClient.get(
			uri: url,
			contentType: ContentType.JSON,
			query: [destroy: true, 'api-version': '5.0-preview.1']
			)
		def att = result.'value'.find { attachment ->
			"${attachment.filename}" == "${filename}"
			
		}
		return att != null
	}
	
	private def getResultsTestcaseMap(def url, boolean justKeys = false) {
		int skip = 0
		def tcMap = [:]
		while (true) {
			def result = genericRestClient.get(
				uri: url,
				contentType: ContentType.JSON,
				query: [destroy: true, 'api-version': '5.0',detailsToInclude:'Iterations,WorkItems', '$top': 200, '$skip': skip]
				)
			skip += 200
			if (!result || !result.'value' || result.'value'.size() == 0) break
			result.'value'.each { aresult ->
				if (aresult.testCase) {
					if (justKeys) {
						tcMap["${aresult.testCase.id}"] = "${aresult.testCase.id}"
					} else {
						tcMap["${aresult.testCase.id}"] = aresult
					}
				}
			}
		}
		return tcMap

	}
	
	public def getResultsMap(def url) {
		int skip = 0
		def rMap = [:]
		while (true) {
			def result = genericRestClient.get(
				uri: url,
				contentType: ContentType.JSON,
				query: [destroy: true, 'api-version': '5.0',detailsToInclude:'WorkItems', '$top': 200, '$skip': skip]
				)
			skip += 200
			if (!result || !result.'value' || result.'value'.size() == 0) break
			result.'value'.each { aresult ->
				rMap["${aresult.id}"] = aresult
			}
		}
		return rMap

	}
	
	def cacheSuiteResults(def suite, def keyMap) {
		String pid = getSuiteId(suite)
		def runData = cacheManagementService.getFromCache(pid, ICacheManagementService.RUN_DATA)
		if (runData) {
			def resultMap = getResultsMap("${runData.url}/results")
			keyMap.each { rid, cKey ->
				String rKey = "${rid}"
				def result = resultMap[rKey]
				if (result) {
					cacheManagementService.saveToCache(result, cKey, ICacheManagementService.RESULT_DATA)
				}
			}
		}
		
	}
	
	public def getTestResultsById(collection, def project, String runID) {
		
		def projectInfo = projectManagmentService.getProject(collection, project)
		//def queryHierarchy = getQueryHierarchy(project)
//		def query = new JsonBuilder( queryHierarchy.queries[0] ).toString()
//
//		def queryJson = [queryJson: query]
//		def body = new JsonBuilder( queryJson ).toString()
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			//requestContentType: ContentType.JSON,
			//uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectInfo.id}/_apis/test/runs/${runID}",
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectInfo.id}/_apis/test/runs/${runID}/results",
			//headers: ['Content-Type': 'application/json'],
			//query: ['api-version':'5.0-preview.2', includeRunDetails: true]
			query: [destroy: true, 'api-version': '5.0-preview.2', detailsToInclude:'workItems', '$top': 200]
			
			)

		return result;

	}
	
	
	
	
	
	
	public def getResult(String uri) {
		def result = genericRestClient.get(
			uri: uri,
			contentType: ContentType.JSON,
			query: [destroy: true, 'api-version': '5.0',detailsToInclude:'workItems,subResults']
			)
		return result
	}

	private String getTestChangeType(def change) {
		String type = ICacheManagementService.PLAN_DATA
		String uri = "${change.uri}"
		if (uri.indexOf('suites') > -1) {
			type = ICacheManagementService.SUITE_DATA
		} else if (uri.indexOf('configurations') > -1) {
			type = ICacheManagementService.CONFIGURATION_DATA
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
	
	
	private def getTestWorkItems(String collection, String project, String teamArea, String inquery = null) {
		String prefix = 'RQM'
		if (cacheManagementService.cacheModule == 'TL') {
			prefix = 'TL'
		}
		def eproject = URLEncoder.encode(project, 'utf-8')
		eproject = eproject.replace('+', '%20')
		String wiql = "Select [System.Id], [System.Title] From WorkItems Where [System.TeamProject] = '${project}' AND [System.WorkItemType] IN ('Test Plan','Test Case') AND [System.AreaPath] UNDER '${teamArea}' AND [Custom.ExternalID] Contains '${prefix}-'"
		def query = [query: wiql]
		if (inquery) {
			query = [query: inquery]
		}
		String body = new JsonBuilder(query).toPrettyString()
		def result = genericRestClient.post(
			requestContentType: ContentType.JSON,
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/wit/wiql",
			body: body,
			//headers: [Accept: 'application/json'],
			query: ['api-version': '5.0-preview.2', '$top': 1000]
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
	
	
	
//	private def associateCaseToPlan(def suiteUrl, def tcids, boolean update = false) {
//		String suiteUrl = "${planData.rootSuite.url}"
//		//def ids = filterTestCaseIds(suiteUrl, tcids)
//		if (tcids.size()>0) {
//			String tcIds = tcids.join(',')
//			addTestCase(suiteUrl, tcIds, update)
//		}
//	}
	
	private def addTestCase(String suiteUrl, String tcIds) {
		String tcUrl = "${suiteUrl}/testcases/${tcIds}"
		
		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			//requestContentType: ContentType.JSON,
			uri: tcUrl,
			query: ['api-version':'5.0']
			)
		return result
	}
	
	private def refreshTestCaseMap(String suiteUrl) {
		String key = suiteUrl.bytes.encodeBase64()
		def tcMap = getSuiteTestCaseMap(suiteUrl)
		cacheManagementService.saveToCache(tcMap, key, 'SuiteTCMap')
		return tcMap
	}
	
	private def filterIds(def idList, def tcMap) {
		Set<String> oid = []
		idList.each { id ->
			String key = "${id}"
			if (!tcMap.containsKey(id)) {
				oid.add(key)
			}
		}
		return oid
	}
	
	public def getSuiteTestCaseMap(String suiteUrl) {
		def tcMap = [:]
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			//requestContentType: ContentType.JSON,
			uri: "${suiteUrl}/testcases",
			query: ['api-version':'5.0']
			)
		if (!result || !result.'value') return tcMap
		result.'value'.each { tc ->
			String id = "${tc.testCase.id}"
			tcMap[id] = tc.testCase
		}
		return tcMap
	}
	
	//not used
//	private def filterTestCaseIds(String url, def ids) {
//		def tcs = getSuiteTestCase(url)
//		def otcs = ids.findAll { id ->
//			boolean excludesId = true
//			tcs.'value'.each { tc ->
//				if ("${tc.id}" == "${id}") {
//					excludesId = false
//					return
//				}
//			}
//			return excludesId
//		}
//		return otcs
//	}
	
	// not used
//	private def getSuiteTestCase(String url) {
//		String tcUrl = "${url}/testcases"
//		def result = genericRestClient.get(
//			contentType: ContentType.JSON,
//			//requestContentType: ContentType.JSON,
//			uri: url,
//			query: ['api-version':'5.0-preview.3']
//			)
//		return result
//
//	}
	
	public def associateCaseToSuite(def suiteUrl, def tcids) {
		if (tcids.size()>0) {
			String tcIds = tcids.join(',')
			addTestCase(suiteUrl, tcIds)
		}
	}
	
	public def associateSuitesToSuite(def suiteData, def tsids) {
		String suiteUrl = "${suiteData.url}"
		if (tsids.size()>0) {
			String tcIds = tsids.join(',')
			addTestCase(suiteUrl, tcIds)
		}
	}
	public def associateSuitesToPlan(def planData, def tsids) {
		String planUrl = "${planData.url}"
		if (tsids.size()>0) {
			addSuitesToPlan(planData, tsids)
		}
	}
	
	private def addSuitesToPlan(def planData, tsids) {
		tsids.each { id ->
			
		}
	}

//	private def getPlanSuites(def planData) {
//		String url = "${planData._links.self}/suites"
//		def result = genericRestClient.get(
//			contentType: ContentType.JSON,
//			//requestContentType: ContentType.JSON,
//			uri: url,
//			query: ['api-version':'5.0-preview.3']
//			)
//		return result
//	}
	
//	private boolean hasSuite(def planData, String id) {
//		def suites = getPlanSuites(planData)
//		Collection suite = suites.'value'.findAll { asuite ->
//			"${asuite.id}" == "${id}"
//		}
//		return suite.size()>0
//	}

	
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
	

	
	def createRunData(String collection, String project, def planData, String buildId = null, boolean automated = false ) {
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def testpoints = getTestPoints(collection, project, planData)
		def data = [name: "${planData.name} Run", plan: [id: planData.id], pointIds:testpoints, automated: automated]
		if (buildId && buildId.size() > 0) {
			data.build = [id: buildId]
		}
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
	
	def createRunDataForTestSuite(String collection, String project, def suiteData, String buildId = null, boolean automated = false) {
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def testpointsMap = getTestPointsForSuiteMap(collection, project, suiteData)
		def testpoints = []
		testpointsMap.each { tid, point ->
			if (!hasManualRun(suiteData, "${tid}")) {
				testpoints.add(point.id)
			}
				
		}
		if (testpoints.size() == 0) return null
		def data = [name: "${suiteData.name} Run", plan: [id: suiteData.plan.id], pointIds:testpoints, automated: automated]
		if (buildId && buildId.size() > 0) {
			data.build = [id: buildId]
		}
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

	def createRunDataForTestCase(String collection, String project, def adoSuiteData, def adoTestCaseData, String buildId = null, boolean automated = false, def testcasePointsMap = null  ) {
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def testpoints = []
		if (!adoTestCaseData) return null
		if (testcasePointsMap) {
			String tid = "${adoTestCaseData.id}"
			def pid = testcasePointsMap[tid]
			testpoints.add(pid)
		} else {
			testpoints = getTestPointsForTestCase(adoSuiteData, adoTestCaseData)
		}
		def data = [name: "${adoSuiteData.name}-${adoTestCaseData.fields.'System.Title'} Run", plan: [id: adoSuiteData.plan.id], pointIds:testpoints, automated: automated]
		if (buildId && buildId.size() > 0) {
			data.build = [id: buildId]
		}
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
		if (!planData) return retVal
		def suites = getPlanTestSuites(collection, project, planData)
		suites.'value'.each { suite ->
			String url = "${suite.url}/points"
			def result = genericRestClient.get(
				contentType: ContentType.JSON,
				//requestContentType: ContentType.JSON,
				uri: url,
				query: ['api-version':'5.0-preview.2']
				)
				
			result.'value'.each { point ->
				retVal.add(point.id)
				
			}
		}
		return retVal
	}
	
	private def getTestPointsForSuiteMap(String collection, String project, def suite) {
		def retVal = [:]
		String url = "${suite.url}/points"
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			//requestContentType: ContentType.JSON,
			uri: url,
			query: ['api-version':'5.0-preview.2']
			)
		if (!result) return [:]
		result.'value'.each { point ->
			retVal[point.testCase.id] = point
			
		}
		return retVal
	}
	
	private def getTestPointsForSuite(def adoSuite) {
		def retVal = []
		int skip = 0
		String url = "${adoSuite.url}/points"
		while (true) {
			def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: url,
				query: ['api-version':'5.0-preview.2', '$top': 200, '$skip': skip]
			)
			
			if (!result || !result.value || result.count == 0) break
			result.value.each { point ->
				retVal.add(point.id)
			}
			skip += 200
		}
		return retVal
	}
	
	private def getTestPointsForTestCase(def adoSuite, def adoTestCase) {
		def retVal = []
		int skip = 0
		String url = "${adoSuite.url}/points"
		if (adoSuite.rootSuite) {
			url = "${adoSuite.rootSuite.url}/points"
		}
		String tid = "${adoTestCase.id}"
		while (true) {
			def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: url,
				query: ['api-version':'5.0-preview.2', '$top': 200, '$skip': skip]
			)
			
			if (!result || !result.value || result.count == 0) break
			result.value.each { point ->
				String ptid = "${point.testCase.id}"
				if (tid == ptid) {
					retVal.add(point.id)
					return
				}
			}
			if (retVal.size() > 0) break
			skip += 200
		}
		return retVal
	}
	
	public def getSuiteTestPointMap(def adoSuite) {
		def retVal = [:]
		int skip = 0
		String url = "${adoSuite.url}/points"
		while (true) {
			def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: url,
				query: ['api-version':'5.0-preview.2', '$top': 200, '$skip': skip]
			)
			
			if (!result || !result.value || result.count == 0) break
			result.value.each { point ->
				String ptid = "${point.testCase.id}"
				retVal[ptid] = point.id
			}
			skip += 200
		}
		return retVal
	}

	private def getTestPoints(String collection, String project, def adoPlanData, def adoTestCaseData ) {
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def retVal = []
		if (!adoPlanData || !adoTestCaseData) return retVal
		int skip = 0
		def data = [PointsFilter: [TestcaseIds:[adoTestCaseData.id]]]
		String body = new JsonBuilder( data ).toString()
		String planUrl = "${adoPlanData.url}"
		if (!adoPlanData.url && adoPlanData._links._self.href) {
			planUrl = "${adoPlanData._links._self.href}"
		}
		while (true) {
			def result = genericRestClient.post(contentType: ContentType.JSON,
				requestContentType: ContentType.JSON,
				uri: "${genericRestClient.tfsUrl}/${collection}/${eproject}/_apis/test/points",
				body: body,
				query: ['api-version':'5.0-preview.2', '$top': 200, '$skip': skip]
			)
			
			if (!result || !result.points || result.points.size() == 0) break
			def fpoints = []
			result.points.each { point ->
				String url = "${point.url}"
				if (url.startsWith(planUrl)) {
					fpoints.push(point)
				}
			}
			if (fpoints && fpoints.size() > 0) {
				fpoints.each { point ->
					retVal.add(point.id)
				}
			}
			skip += 200
		}
		return retVal
	}

	private def getPlanTestSuites(String collection, String project, def planData) {
		def retVal
		if (!planData) return retVal
		String url = "${planData.url}/suites"
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			//requestContentType: ContentType.JSON,
			uri: url,
			query: ['api-version':'5.0', '$expand':'true']
			)
		return result
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
