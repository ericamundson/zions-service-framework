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
		if (result != null) {
			def eResult = getResult(nuri)
			cacheManagementService.saveToCache(eResult, id, ICacheManagementService.RESULT_DATA)
		}
//		if (result == null) {
//			checkpointManagementService.addLogentry("Unable to save test result with id:  ${id}")
//		}
		return result
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
	 * <img src="TestManagmentService_sendPlanChanges.png"/>
	 * 
	 * @param collection - ADO organization
	 * @param tfsProject - ADO project
	 * @param change - ADO plan item request data
	 * @param id - cache item ID
	 * @return ADO request result.
	 * 
	 * @startuml TestManagmentService_sendPlanChanges.png
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
						genericRestClient.delete(
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
	
	def getPlan(String collection, String project, String planName) {
		
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/testplan/plans",
			query: ['api-version':'5.0-preview.1']
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
	
	String getTestCaseId(def testcaseData) {
		String className = testcaseData.getClass().simpleName
		if (className == 'TestCase') {
			return "${testcaseData.id}"
		}
		return "${testcaseData.webId.text()}-Test Case"
	}

	public def ensureTestRun(String collection, String project, def planData) {
		String pid = getPlanId(planData)
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
	
	public def ensureTestRun(String collection, String project, def planData, def testcaseData) {
		String pid = getPlanId(planData)
		String tcid = getTestCaseId(testcaseData)
		String key = "${tcid}_${pid}"
		def runData = cacheManagementService.getFromCache(key, ICacheManagementService.RUN_DATA)
		
		if (runData == null) {
			def adoPlanData = cacheManagementService.getFromCache(pid, ICacheManagementService.PLAN_DATA)
			def adoTestCaseData = cacheManagementService.getFromCache(tcid, ICacheManagementService.WI_DATA)
			
			runData = createRunData(collection, project, adoPlanData, adoTestCaseData)
			if (runData != null) {
				cacheManagementService.saveToCache(runData, key, ICacheManagementService.RUN_DATA)
			}
		}
		def resultTestCaseMap = getResultsTestcaseMap("${runData.url}/results")
		return resultTestCaseMap
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
	
	def getResultsTestcaseMap(def url) {
		int skip = 0
		def tcMap = [:]
		while (true) {
			def result = genericRestClient.get(
				uri: url,
				contentType: ContentType.JSON,
				query: [destroy: true, 'api-version': '5.0',detailsToInclude:'WorkItems', '$top': 200, '$skip': skip]
				)
			skip += 200
			if (!result || !result.'value' || result.'value'.size() == 0) break
			result.'value'.each { aresult ->
				tcMap["${aresult.testCase.id}"] = aresult
			}
		}
		return tcMap

	}
	
	private def getResult(String uri) {
		def result = genericRestClient.get(
			uri: uri,
			contentType: ContentType.JSON,
			query: [destroy: true, 'api-version': '5.0',detailsToInclude:'WorkItems']
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
			query: ['api-version':'5.0']
			)
		return result
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
	
	public def associateCaseToSuite(def suiteData, def tcids) {
		String suiteUrl = "${suiteData.url}"
		if (tcids.size()>0) {
			String tcIds = tcids.join(',')
			addTestCase(suiteUrl, tcIds)
		}
	}
	
	public def associateSuitesToSuite(def suiteData, def tsids) {
		String suiteUrl = "${suiteData.url}"
		if (tcids.size()>0) {
			String tcIds = tcids.join(',')
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
	

	
	def createRunData(String collection, String project, def planData ) {
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
	
	def createRunData(String collection, String project, def adoPlanData, def adoTestCaseData ) {
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def testpoints = getTestPoints(collection, project, adoPlanData, adoTestCaseData)
		def data = [name: "${adoPlanData.name}-${adoTestCaseData.fields.'System.Title'} Run", plan: [id: adoPlanData.id], pointIds:testpoints]
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
	private def getTestPoints(String collection, String project, def adoPlanData, def adoTestCaseData ) {
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def retVal = []
		if (!adoPlanData || !adoTestCaseData) return retVal
		int skip = 0
		def data = [PointsFilter: [TestcaseIds:[adoTestCaseData.id]]]
		String body = new JsonBuilder( data ).toString()
		String planUrl = "${adoPlanData.url}"
		while (true) {
			def result = genericRestClient.post(contentType: ContentType.JSON,
				requestContentType: ContentType.JSON,
				uri: "${genericRestClient.tfsUrl}/${collection}/${eproject}/_apis/test/points",
				body: body,
				query: ['api-version':'5.0-preview.2', '$top': 200, '$skip': skip]
			)
			
			if (!result || !result.points || result.points.size() == 0) break
			def fpoints = result.points.findAll { point ->
				String url = "${point.url}"
				true
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
