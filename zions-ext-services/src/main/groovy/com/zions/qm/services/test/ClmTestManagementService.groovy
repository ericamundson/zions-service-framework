package com.zions.qm.services.test

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import com.zions.common.services.cacheaspect.Cache
import com.zions.common.services.link.LinkInfo

import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cacheaspect.CacheInterceptor
import com.zions.common.services.cacheaspect.CacheWData
import com.zions.common.services.rest.IGenericRestClient
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import groovy.xml.XmlUtil
import groovyx.net.http.ContentType

/**
 * o Handle RQM test queries.
 * 
 * @author z091182
 *
 */
@Component
@Slf4j
class ClmTestManagementService {
	
	@Autowired
	IGenericRestClient qmGenericRestClient
	
	@Autowired(required=false)
	ICacheManagementService cacheManagementService
	
	@Value('${qm.query:}')
	String qmQuery
	
	@Value('${qm.tc.query:}')
	String qmTcQuery

	
	public ClmTestManagementService() {
		
	}
	
	
	
	/**
	 * Retrieve custom attribute meta data from QM CLM project.
	 * 
	 * @param projectName - CLM QM project name.
	 * @param type - specific QM type
	 * @return Custom attribute data
	 */
	public def getCustomAttributes(String projectName, String type)
	{
		
		Map<String, String> typeMap = ['Test Case': 'TEST_CASE', 'Test Suite': 'TEST_SUITE', 'Test Plan': 'TEST_PLAN']
		String scope = typeMap[type]
//		uriBuilder.addParameter("scope", 'TEST_CASE')
//		uriBuilder.addParameter("resolveValues", 'false')
//		uriBuilder.addParameter("isNotPurged", 'true')
//		uriBuilder.addParameter("processAreaUUID", paId)
		
//
		def pa = getProjectArea(projectName)
		if (pa != null) {
			String url = this.qmGenericRestClient.clmUrl + "/qm/service/com.ibm.rqm.planning.common.service.rest.ICustomAttributeRestService/customAttributesDTO"
			def result = qmGenericRestClient.get( uri: url,
				headers: [Accept: 'text/json'],
				query: [scope: scope, resolveValues: false, isNotPurged: true, processAreaUUID: pa.itemId])
			def ca = new JsonSlurper().parse(result)
			return ca
		}
		return null
	}
	
	public def getCategories(String projectName, String type)
	{
		Map<String, String> typeMap = ['Test Case': 'TestCase', 'Test Suite': 'TestSuite', 'Test Plan': 'TestPlan']
		String itemType = typeMap[type]
//		uriBuilder.addParameter("scope", 'TEST_CASE')
//		uriBuilder.addParameter("resolveValues", 'false')
//		uriBuilder.addParameter("isNotPurged", 'true')
//		uriBuilder.addParameter("processAreaUUID", paId)
		
//
		def pa = getProjectArea(projectName)
		if (pa != null) {
			String url = this.qmGenericRestClient.clmUrl + "/qm/service/com.ibm.rqm.planning.common.service.rest.ICategoryTypeRestService/categoryTypesDTO"
			def result = qmGenericRestClient.get( uri: url,
				headers: [accept: 'text/json'],
				query: [itemType: itemType, resolveCategories: true, includeGlobal: false, isNotPurged: true, processAreaUUID: pa.itemId])
			def categories = new JsonSlurper().parse(result)
			return categories
		}
		return null
	}

	def getTestItem(String uri) {
		uri = uri.replace(' ', '+')
		def result = qmGenericRestClient.get(
			//contentType: ContentType.XML,
			uri: uri,
			query: [calmlinks: true],
			headers: [Accept: 'text/xml'] );
		return result
	
	}
	

	
	def getProjectArea(String name) {
		def thepa = null
		String url = this.qmGenericRestClient.clmUrl + "/qm/service/com.ibm.team.process.internal.service.web.IProcessWebUIService/allProjectAreas"
		def pasStream = qmGenericRestClient.get( uri: url,
			//contentType: ContentType.XML,
			headers: [Accept: 'text/json'],
			query: [userId: this.qmGenericRestClient.userid])
		def pas = new JsonSlurper().parse(pasStream)
		String json = new JsonBuilder(pas).toPrettyString()
//			URIBuilder uriBuilder = new URIBuilder(url);
//			uriBuilder.addParameter("hideArchivedProjects", 'true')
//			uriBuilder.addParameter("owningApplicationKey", "JTS-Sentinel-Id")
//			uriBuilder.addParameter("pageNum", ""+ pageNum)
//			uriBuilder.addParameter("pageSize", "" + pageSize)
//			HttpGet httpget = new HttpGet(uriBuilder.build());
//			httpget.setHeader("accept", "text/json");

		pas.'soapenv:Body'.response.returnValue.values.each { pa ->
			if ("${pa.summary}" == name) {
				thepa = pa
			}
		}
		return thepa;
	}
	
	def getContent(String uri) {
		def result = qmGenericRestClient.get(
			withHeader: true,
			uri: uri,
			contentType: ContentType.BINARY
			);
		String cd = "${result.headers.'Content-Disposition'}"
		
		String[] sItem = cd.split('=')
		String filename = null
		if (sItem.size() == 2) {
			filename = sItem[1]
			filename = filename.replace('"', '')
		}
		def outData = [filename: filename, data: result.data]
		return outData

	}
	
	def flushQueries(String projectName, int maxPage = -1) {
		Date ts = new Date()
		cacheManagementService.saveToCache([timestamp: ts.format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")], 'query', 'QueryStart')
		int page = 0
		
		def planData
		new CacheInterceptor() {}.provideCaching(this, "${page}", ts, TestPlanQueryData) {
			planData = this.getTestPlansViaQuery(qmQuery, projectName)
		}
		while (true) {
			page++
			if (maxPage != -1 && maxPage == page) break; // For testing
			def nextLink = planData.'**'.find { node ->
				node.name() == 'link' && node.@rel == 'next'
			}
			if (nextLink == null) break
			new CacheInterceptor() {}.provideCaching(this, "${page}", ts, TestPlanQueryData) {
				planData = this.nextPage(nextLink.@href)
			}
		}
		
		
		
		page = 0
		def configData 
		new CacheInterceptor() {}.provideCaching(this, "${page}", ts, ConfigurationQueryData) {
			configData = this.getConfigurationsViaQuery('', projectName)
		}
		while (true) {
			page++
			if (maxPage != -1 && maxPage == page) break; // For testing
			def nextLink = configData.'**'.find { node ->
				node.name() == 'link' && node.@rel == 'next'
			}
			if (nextLink == null) break
			new CacheInterceptor() {}.provideCaching(this, "${page}", ts, ConfigurationQueryData) {
				configData = this.nextPage(nextLink.@href)
			}
		}
		
		page = 0
		def testcaseData
		new CacheInterceptor() {}.provideCaching(this, "${page}", ts, TestCaseQueryData) {
			testcaseData = this.getTestCaseViaQuery(qmTcQuery, projectName)
		}
		while (true) {
			page++
			if (maxPage != -1 && (maxPage) == page) break; // For testing
			def nextLink = testcaseData.'**'.find { node ->
				node.name() == 'link' && node.@rel == 'next'
			}
			if (nextLink == null) break
			new CacheInterceptor() {}.provideCaching(this, "${page}", ts, TestCaseQueryData) {
				testcaseData = this.nextPage(nextLink.@href)
			}
		}

	}

	def getTestPlansViaQuery(String query, String projectName) {
		def encoded = URLEncoder.encode(query, 'UTF-8')
		encoded = encoded.replace('+', '%20')
		def project = URLEncoder.encode(projectName, 'UTF-8')
		project = project.replace('+', '%20')

		String uri = this.qmGenericRestClient.clmUrl + "/qm/service/com.ibm.rqm.integration.service.IIntegrationService/resources/${project}/testplan?fields=" + encoded;
		if (query == null || query.length() == 0 || "${query}" == 'none') {
			uri = this.qmGenericRestClient.clmUrl + "/qm/service/com.ibm.rqm.integration.service.IIntegrationService/resources/${project}/testplan";
			
		}
		def result = qmGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'application/xml'] );
		return result
	}
	
	def getTestCaseViaQuery(String query, String projectName) {
		def encoded = URLEncoder.encode(query, 'UTF-8')
		encoded = encoded.replace('+', '%20')
		def project = URLEncoder.encode(projectName, 'UTF-8')
		project = project.replace('+', '%20')

		String uri = this.qmGenericRestClient.clmUrl + "/qm/service/com.ibm.rqm.integration.service.IIntegrationService/resources/${project}/testcase?fields=" + encoded;
		if (query == null || query.length() == 0 || "${query}" == 'none') {
			uri = this.qmGenericRestClient.clmUrl + "/qm/service/com.ibm.rqm.integration.service.IIntegrationService/resources/${project}/testcase";
			
		}
		def result = qmGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'application/xml'] );
		return result
	}

	def getConfigurationsViaQuery(String query, String projectName) {
		def encoded = URLEncoder.encode(query, 'UTF-8')
		encoded = encoded.replace('+', '%20')
		def project = URLEncoder.encode(projectName, 'UTF-8')
		project = project.replace('+', '%20')

		String uri = this.qmGenericRestClient.clmUrl + "/qm/service/com.ibm.rqm.integration.service.IIntegrationService/resources/${project}/configuration?fields=" + encoded;
		if (query == null || query.length() == 0 || "${query}" == 'none') {
			uri = this.qmGenericRestClient.clmUrl + "/qm/service/com.ibm.rqm.integration.service.IIntegrationService/resources/${project}/configuration";
			
		}
		def result = qmGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'application/xml'] );
//		String resultsxml = XmlUtil.serialize(result)
//		File resultFile = new File('../zions-ext-services/src/test/resources/testdata/configurations.xml')
//		def os = resultFile.newDataOutputStream()
//		os << resultsxml
//		os.close()
		return result
	}

	def getExecutionResultViaHref(String tcWebId, String planWebId, String projectName) {
		def project = URLEncoder.encode(projectName, 'UTF-8')
		//project = project.replace('+', '%20')
		String tchref = this.qmGenericRestClient.clmUrl + "/qm/service/com.ibm.rqm.integration.service.IIntegrationService/resources/${project}/testcase/urn:com.ibm.rqm:testcase:${tcWebId}"
		String tphref = this.qmGenericRestClient.clmUrl + "/qm/service/com.ibm.rqm.integration.service.IIntegrationService/resources/${project}/testplan/urn:com.ibm.rqm:testplan:${planWebId}"
		def outItems = []
		String query = "feed/entry/content/executionresult[testcase/@href='${tchref}' and testplan/@href='${tphref}']/*"
		String uri = this.qmGenericRestClient.clmUrl + "/qm/service/com.ibm.rqm.integration.service.IIntegrationService/resources/${project}/executionresult";
		def result = qmGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'application/xml'],
				query: [fields: query] );
			
		// generate unit test data.
//		String resultsxml = XmlUtil.serialize(result)
//		File resultFile = new File('../zions-ext-services/src/test/resources/testdata/executionresults1.xml')
//		def os = resultFile.newDataOutputStream()
//		os << resultsxml
//		os.close()
		
		while (true) {
			def erlist = result.'**'.findAll { it.name() == 'executionresult' }
			
			erlist.each { item ->
				String itemxml = XmlUtil.serialize(item)
				outItems.add(item)
			}
			def nextLink = result.'**'.find { node ->
				
				node.name() == 'link' && node.@rel == 'next'
			}
			if (nextLink == null) break
			result = nextPage(nextLink.@href)

		}
		return outItems
	}

	public def nextPage(url) {
		def result = qmGenericRestClient.get(
			uri: url,
			headers: [Accept: 'application/xml'] );
		return result
	}

}

class ConfigurationQueryData implements CacheWData {
	String data
	
	void doData(def result) {
		data = new XmlUtil().serialize(result)
	}
	
	def dataValue() {
		return new XmlSlurper().parseText(data)
	}

}

class TestCaseQueryData implements CacheWData {
	String data
	
	void doData(def result) {
		data = new XmlUtil().serialize(result)
	}
	
	def dataValue() {
		return new XmlSlurper().parseText(data)
	}

}

class TestPlanQueryData implements CacheWData {
	String data
	
	void doData(def result) {
		data = new XmlUtil().serialize(result)
	}
	
	def dataValue() {
		return new XmlSlurper().parseText(data)
	}

}