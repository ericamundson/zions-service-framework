package com.zions.qm.services.cli.action.test.query

import com.zions.common.services.cacheaspect.CacheInterceptor
import com.zions.qm.services.test.TestCaseQueryData
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class TestcaseQueryHandler extends BaseQueryHandler {
	@Value('${testcase.item.filter:allFilter}')
	String tcItemFitler
	
	public def getItems() {
		def cp = cacheManagementService.getFromCache('query', 'QueryStart')
		page=0
		if (cp) {
			currentTimestamp = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", cp.timestamp)
			
		}
		String pageId = "${page}"
		new CacheInterceptor() {}.provideCaching(clmTestManagementService, pageId, currentTimestamp, TestCaseQueryData) {
			currentItems = clmTestManagementService.getTestCaseViaQuery(this.qmTcQuery, projectName)
		}
		return currentItems
	}
	public String initialUrl() {
		def query = ''
		def encoded = URLEncoder.encode(this.qmTcQuery, 'UTF-8')
		encoded = encoded.replace('+', '%20')
		def project = URLEncoder.encode(projectName, 'UTF-8')
		//project = project.replace('+', '%20')

		String uri = this.qmGenericRestClient.clmUrl + "/qm/service/com.ibm.rqm.integration.service.IIntegrationService/resources/${project}/testcase?fields=" + encoded;
		if (query == null || query.length() == 0 || "${query}" == 'none') {
			uri = this.qmGenericRestClient.clmUrl + "/qm/service/com.ibm.rqm.integration.service.IIntegrationService/resources/${project}/testcase";
			
		}
		return uri
	}
	
	public String getFilterName() {
		// TODO Auto-generated method stub
		return this.tcItemFitler;
	}

	public Object nextPage() {
		def nextLink = currentItems.'**'.find { node ->
	
			node.name() == 'link' && node.@rel == 'next'
		}
		if (nextLink == null) return null
		this.page++
		String pageId = "${page}"
		new CacheInterceptor() {}.provideCaching(clmTestManagementService, pageId, currentTimestamp, TestCaseQueryData) {
			currentItems = clmTestManagementService.nextPage(nextLink.@href)
		}
		return currentItems
	}

}
