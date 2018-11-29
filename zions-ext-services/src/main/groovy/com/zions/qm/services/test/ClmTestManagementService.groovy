package com.zions.qm.services.test

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.zions.common.services.rest.IGenericRestClient
import groovy.xml.XmlUtil

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
	
	def getTestPlansViaQuery(String query, String projectName) {
		def encoded = URLEncoder.encode(query, 'UTF-8')
		encoded = encoded.replace('+', '%20')
		def project = URLEncoder.encode(projectName, 'UTF-8')
		//project = project.replace('+', '%20')

		String uri = this.qmGenericRestClient.qmUrl + "/qm/service/com.ibm.rqm.integration.service.IIntegrationService/resources/${project}/testplan?fields=" + encoded;
		if (query == null || query.length() == 0 || "${query}" == 'none') {
			uri = this.qmGenericRestClient.qmUrl + "/qm/service/com.ibm.rqm.integration.service.IIntegrationService/resources/${project}/testplan";
			
		}
		def result = qmGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'application/xml'] );
		return result
	}
	
	def getExecutionResultViaHref(String tchref, String planhref, String projectName) {
		def project = URLEncoder.encode(projectName, 'UTF-8')
		//project = project.replace('+', '%20')
		def outItems = []
		String query = "feed/entry/content/executionresult[testcase/@href='${tchref}']/*"
		String uri = this.qmGenericRestClient.qmUrl + "/qm/service/com.ibm.rqm.integration.service.IIntegrationService/resources/${project}/executionresult";
		def result = qmGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'application/xml'],
				query: [fields: query] );
		String resultsxml = XmlUtil.serialize(result)
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
