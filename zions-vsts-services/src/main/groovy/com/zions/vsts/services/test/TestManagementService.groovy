package com.zions.vsts.services.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient;
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType

/**
 * @author z091182
 *
 */
@Component
public class TestManagementService {
	@Autowired(required=true)
	private GenericRestClient genericRestClient;
	
	@Autowired(required=true)
	private ProjectManagementService projectManagmentService;

	@Autowired
	@Value('${cache.location}')
	String cacheLocation
	
	public TestManagementService() {
		
	}
	
	def sendResultChanges(String collection, String project, def executionResult, String id) {
		String method = "${executionResult.method}"
		executionResult.remove('method')
		def result = null
		String nuri = "${genericRestClient.getTfsUrl()}${executionResult.uri}"
		executionResult.uri = nuri
		if (method == 'post') {
			result = genericRestClient.post(executionResult)
		} else if (method == 'patch') {
			result = genericRestClient.patch(executionResult)
		}
		if (result != null) {
			this.saveResultState(result, id)
		}
	}
	
	def batchPlanChanges(String collection, String tfsProject, def changeList, def idMap) {
		int count = 0
		changeList.each { change ->
			String nuri = "${genericRestClient.getTfsUrl()}${change.uri}"
			change.uri = nuri
			String method = "${change.method}"
			
			((Map) change).remove('method')
			def result = null
			if (method == 'post') {
				result = genericRestClient.post(change)
			} else if (method == 'patch') {
				result = genericRestClient.patch(change)
			}
			if (result != null) {
				saveState(result, idMap[count])
			}
			count++
		}
	}
	
	def sendPlanChanges(String collection, String tfsProject, def change, String id) {
		String nuri = "${genericRestClient.getTfsUrl()}${change.uri}"
		change.uri = nuri
		String method = "${change.method}"
		
		((Map) change).remove('method')
		def result = null
		if (method == 'post') {
			result = genericRestClient.post(change)
		} else if (method == 'patch') {
			result = genericRestClient.patch(change)
		}
		if (result != null) {
			saveState(result, id)
		}
		return result
	}

	
	def getQueryHierarchy(def project) {
		def collection = ""
		def projectInfo = projectManagmentService.getProject(collection, project)
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectInfo.id}/_api/_TestQueries/GetQueryHierarchy",
			headers: ['Content-Type': 'application/json'],
			query: [itemTypes: 'ExploratorySession', itemTypes: 'TestResult', itemTypes: 'TestRun']
			)
		return result
	}
	
	def cleanupTestItems(String collection, String project, String teamArea) {
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
		File cacheL = new File(cacheLocation)
		cacheL.deleteDir()
	}
	
	def getTestWorkItems(String collection, String project, String teamArea) {
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
	
	def getWorkItem(String url) {
		def result = genericRestClient.get(
			uri: url,
			contentType: ContentType.JSON,
			query: [destroy: true, 'api-version': '5.0-preview.3']
			)
		return result
	}
	
	def getTestRuns(def project) {
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
	
	def setParent(def parent, def children, def map) {
		String pname = "${parent.name()}"
		String ptname = getTargetName(pname, map)
		String pid = "${parent.webId.text()}-${ptname}"
		def parentData = getCacheData(pid)
		if (parentData != null) {
			def tcIds = []
			int tot = children.size()
			int count = 0
			children.each { child -> 
				String cname = "${child.name()}"
				
				String ctname = getTargetName(cname, map)
				
				
				String cid = "${child.webId.text()}-${ctname}"
				def childData = getCacheData(cid)
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
	
	
	def associateCaseToPlan(def planData, def tcids) {
		String suiteUrl = "${planData.rootSuite.url}"
		//def ids = filterTestCaseIds(suiteUrl, tcids)
		if (tcids.size()>0) {
			String tcIds = tcids.join(',')
			addTestCase(suiteUrl, tcIds)
		}
	}
	def addTestCase(String suiteUrl, String tcIds) {
		String tcUrl = "${suiteUrl}/testcases/${tcIds}"
		
		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			//requestContentType: ContentType.JSON,
			uri: tcUrl,
			query: ['api-version':'5.0-preview.3']
			)
		return result
	}
	def filterTestCaseIds(String url, def ids) {
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
	
	def getSuiteTestCase(String url) {
		String tcUrl = "${url}/testcases"
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			//requestContentType: ContentType.JSON,
			uri: url,
			query: ['api-version':'5.0-preview.3']
			)
		return result

	}
	
	def associateCaseToSuite(def suiteData, def tcids) {
		String suiteUrl = "${suiteData.url}"
		def ids = filterTestCaseIds(suiteUrl, tcids)
		if (ids.size()>0) {
			String tcIds = ids.join(',')
			addTestCase(suiteUrl, tcIds)
		}
	}
	
	def getPlanSuites(def planData) {
		String url = "${planData._links.self}/suites"
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			//requestContentType: ContentType.JSON,
			uri: url,
			query: ['api-version':'5.0-preview.3']
			)
		return result
	}
	
	boolean hasSuite(def planData, String id) {
		def suites = getPlanSuites(planData)
		Collection suite = suites.'value'.findAll { asuite ->
			"${asuite.id}" == "${id}"
		}
		return suite.size()>0
	}

	
	String getTargetName(String name, def map) {
		def maps = map.findAll { amap ->
			"${amap.source}" == "${name}"
		}
		if (maps.size() > 0) {
			String retVal = "${maps[0].target}"
			return retVal
		}
		return null

	}
	
	def saveState(def wi, String id) {
		File cacheDir = new File(this.cacheLocation)
		if (!cacheDir.exists()) {
			cacheDir.mkdir();
		}
		File wiDir = new File("${this.cacheLocation}${File.separator}${id}")
		if (!wiDir.exists()) {
			wiDir.mkdir()
		}
		File cacheData = new File("${this.cacheLocation}${File.separator}${id}${File.separator}wiData.json");
		def w  = cacheData.newDataOutputStream()
		w << new JsonBuilder(wi).toPrettyString()
		w.close()
	}
	def saveRunDataState(def runData, String id) {
		File cacheDir = new File(this.cacheLocation)
		if (!cacheDir.exists()) {
			cacheDir.mkdir();
		}
		File wiDir = new File("${this.cacheLocation}${File.separator}${id}")
		if (!wiDir.exists()) {
			wiDir.mkdir()
		}
		File cacheData = new File("${this.cacheLocation}${File.separator}${id}${File.separator}runData.json");
		def w  = cacheData.newDataOutputStream()
		w << new JsonBuilder(runData).toPrettyString()
		w.close()
	}

	def saveResultState(def runData, String id) {
		File cacheDir = new File(this.cacheLocation)
		if (!cacheDir.exists()) {
			cacheDir.mkdir();
		}
		File wiDir = new File("${this.cacheLocation}${File.separator}${id}")
		if (!wiDir.exists()) {
			wiDir.mkdir()
		}
		File cacheData = new File("${this.cacheLocation}${File.separator}${id}${File.separator}resultData.json");
		def w  = cacheData.newDataOutputStream()
		w << new JsonBuilder(runData).toPrettyString()
		w.close()
	}

	def ensureTestRun(String collection, String project, def planData) {
		String pid = "${planData.webId.text()}-Test Plan"
		def runData = getCacheRunData(pid)
		
		if (runData == null) {
			def parentData = getCacheData(pid)
		
			runData = createRunData(collection, project, parentData)
			if (runData != null) {
				saveRunDataState(runData, pid)
			}
		}
		return runData
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

	def getTestPoints(String collection, String project, def planData ) {
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

	def getCacheData(String id) {
		File cacheData = new File("${this.cacheLocation}${File.separator}${id}${File.separator}wiData.json");
		if (cacheData.exists()) {
			JsonSlurper s = new JsonSlurper()
			return s.parse(cacheData)
		}
		return null

	}
	
	def getCacheRunData(String id) {
		File cacheData = new File("${this.cacheLocation}${File.separator}${id}${File.separator}runData.json");
		if (cacheData.exists()) {
			JsonSlurper s = new JsonSlurper()
			return s.parse(cacheData)
		}
		return null

	}
	def getResultData(String id) {
		File cacheData = new File("${this.cacheLocation}${File.separator}${id}${File.separator}resultData.json");
		if (cacheData.exists()) {
			JsonSlurper s = new JsonSlurper()
			return s.parse(cacheData)
		}
		return null

	}
}
