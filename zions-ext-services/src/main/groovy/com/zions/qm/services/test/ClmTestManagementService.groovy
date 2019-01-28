package com.zions.qm.services.test

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.zions.common.services.rest.IGenericRestClient
import groovy.xml.XmlUtil
import groovyx.net.http.ContentType

/**
 * o Handle RQM test queries.
 * 
 * @author z091182
 *
 */
@Component
class ClmTestManagementService {
	
	@Autowired
	IGenericRestClient qmGenericRestClient

	public ClmTestManagementService() {
		
	}
	
	def getTestItem(String uri) {
		def result = qmGenericRestClient.get(
			uri: uri,
			headers: [Accept: 'text/xml'] );
		return result
	
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

	def getTestPlansViaQuery(String query, String projectName) {
		def encoded = URLEncoder.encode(query, 'UTF-8')
		encoded = encoded.replace('+', '%20')
		def project = URLEncoder.encode(projectName, 'UTF-8')
		//project = project.replace('+', '%20')

		String uri = this.qmGenericRestClient.clmUrl + "/qm/service/com.ibm.rqm.integration.service.IIntegrationService/resources/${project}/testplan?fields=" + encoded;
		if (query == null || query.length() == 0 || "${query}" == 'none') {
			uri = this.qmGenericRestClient.clmUrl + "/qm/service/com.ibm.rqm.integration.service.IIntegrationService/resources/${project}/testplan";
			
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
		//project = project.replace('+', '%20')

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
