package com.zions.qm.services.cli.action.test.query

import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.restart.IQueryHandler
import com.zions.qm.services.test.ClmTestManagementService
import com.zions.qm.services.test.TestPlanQueryData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cacheaspect.CacheInterceptor


class BaseQueryHandler implements IQueryHandler {
	
	@Autowired
	ClmTestManagementService clmTestManagementService
	
	@Autowired(required=false)
	ICacheManagementService cacheManagementService

	@Autowired
	IGenericRestClient qmGenericRestClient

	@Value('${qm.query:}')
	String qmQuery
	
	@Value('${qm.tc.query:}')
	String qmTcQuery

	@Value('${clm.projectArea:}')
	String projectName
	
	@Value('${item.filter:qmAllFilter}')
	private String itemFilter

	def currentItems
	
	int page = 0
	
	Date currentTimestamp = new Date()
	
	public def getItems() {
		def cp = cacheManagementService.getFromCache('query', 'QueryStart')
		page=0
		if (cp) {
			currentTimestamp = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", cp.timestamp)
			
		}
		String pageId = "${page}"
		new CacheInterceptor() {}.provideCaching(clmTestManagementService, pageId, currentTimestamp, TestPlanQueryData) {
			currentItems = clmTestManagementService.getTestPlansViaQuery(qmQuery, projectName)
		}
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

		String aUrl = nextLink.@href
		String outUrl = aUrl
		if (aUrl.indexOf('&page=') > -1) {
			outUrl = aUrl.substring(0, aUrl.indexOf('?')+1) + aUrl.substring(aUrl.indexOf('&page=')+1)
		}
		return outUrl
	}

	public Object nextPage() {
		def nextLink = currentItems.'**'.find { node ->
	
			node.name() == 'link' && node.@rel == 'next'
		}
		if (nextLink == null) return null
		this.page++
		String pageId = "${page}"
		new CacheInterceptor() {}.provideCaching(clmTestManagementService, pageId, currentTimestamp, TestPlanQueryData) {
			currentItems = clmTestManagementService.nextPage(nextLink.@href)
		}
		return currentItems
	}

	
	public String getFilterName() {
		// TODO Auto-generated method stub
		return this.itemFilter;
	}
	
	public Date modifiedDate(Object item) {
		String sDate = "${item.updated.text()}"
		
		return new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sDate);
	}


}
