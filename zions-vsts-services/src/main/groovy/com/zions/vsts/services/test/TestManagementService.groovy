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
				} else if (type == 'Test Suite'){
					String url = "${genericRestClient.getTfsUrl()}/${eproject}/_apis/testplan/plans/99999999/suites/${wi.id}"
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
		def query = [query: "Select [System.Id], [System.Title] From WorkItems Where ([System.WorkItemType] = 'Test Plan' OR [System.WorkItemType] = 'Test Suite' OR [System.WorkItemType] = 'Test Suite') AND [System.AreaPath] = '${teamArea}'"]
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
	
	def setParent(def parent, def child, def map) {
		String pname = "${parent.name()}"
		String cname = "${child.name()}"
		
		String ptname = getTargetName(pname, map)
		String ctname = getTargetName(cname, map)
		
		String pid = "${parent.webId.text()}-${ptname}"
		def parentData = getCacheData(pid)
		
		String cid = "${child.webId.text()}-${cname}"
		def childData = getCacheData(cid)
		
		if ("${pname}" == 'testplan' && "${cname}" == 'testsuite') {
			associateSuiteToPlan(parentData, childData)
		} else if ("${pname}" == 'testplan' && "${cname}" == 'testcase') {
			associateCaseToPlan(parentData, childData)
		} else if ("${pname}" == 'testsuite' && "${cname}" == 'testcase') {
			associateCaseToSuite(parentData, childData)
		}
		
	}
	
	def associateSuiteToPlan(def planData, def suiteData) {
		String suiteId = "${suiteData.id}"
		if (!hasSuite(planData, suiteId)) {
			//String uri = 
		}
	}
	
	def associateCaseToPlan(def planData, def caseData) {
	
	}
	
	def associateCaseToSuite(def suiteData, def caseData) {
	
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
		def maps = testMappingManagementService.mappingData.findAll { amap ->
			"${amap.source}" == "${name}"
		}
		if (maps.size() > 0) {
			String retVal = "${maps[0].@target}"
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

	
	def ensureTestPlan(String collection, String project, def planData) {
		
	}

	def getCacheData(String id) {
		File cacheData = new File("${this.cacheLocation}${File.separator}${id}${File.separator}wiData.json");
		if (cacheData.exists()) {
			JsonSlurper s = new JsonSlurper()
			return s.parse(cacheData)
		}
		return null

	}
}
