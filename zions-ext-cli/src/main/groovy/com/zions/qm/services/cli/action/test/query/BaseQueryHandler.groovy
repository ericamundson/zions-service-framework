package com.zions.qm.services.cli.action.test.query

import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.restart.IQueryHandler
import com.zions.qm.services.test.ClmTestManagementService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

class BaseQueryHandler implements IQueryHandler {
	
	@Autowired
	ClmTestManagementService clmTestManagementService
	
	@Autowired
	IGenericRestClient qmGenericRestClient

	@Value('${qm.query:}')
	String qmQuery
	
	@Value('${clm.projectArea:}')
	String projectName
	
	def currentItems
	
	public def getItems() {
		currentItems = clmTestManagementService.getTestPlansViaQuery(qmQuery, projectName)
		return currentItems
	}

	public String initialUrl() {
		def encoded = URLEncoder.encode(qmQuery, 'UTF-8')
		encoded = encoded.replace('+', '%20')
		def project = URLEncoder.encode(projectName, 'UTF-8')
		//project = project.replace('+', '%20')

		String uri = this.qmGenericRestClient.clmUrl + "/qm/service/com.ibm.rqm.integration.service.IIntegrationService/resources/${project}/testplan?fields=" + encoded;
		if (qmQuery == null || qmQuery.length() == 0 || "${qmQuery}" == 'none') {
			uri = this.qmGenericRestClient.clmUrl + "/qm/service/com.ibm.rqm.integration.service.IIntegrationService/resources/${project}/testplan";
			
		}
		return uri
	}

	public String getPageUrl() {
		def nextLink = currentItems.'**'.find { node ->
	
			node.name() == 'link' && node.@rel == 'next'
		}
		if (nextLink == null) return null
		return nextLink.@href
	}

	public Object nextPage() {
		def nextLink = currentItems.'**'.find { node ->
	
			node.name() == 'link' && node.@rel == 'next'
		}
		if (nextLink == null) return null
		currentItems = clmTestManagementService.nextPage(nextLink.@href)
		return currentItems
	}

}
