package com.zions.clm.services.cli.action.work.query

import com.zions.clm.services.rtc.project.workitems.ClmWorkItemManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.restart.IQueryHandler
import com.zions.qm.services.test.ClmTestManagementService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

class BaseQueryHandler implements IQueryHandler {
	
	@Autowired
	ClmWorkItemManagementService clmWorkItemManagementService
	
	@Value('${item.filter:allFilter}')
	private String itemFilter

	@Autowired
	IGenericRestClient clmGenericRestClient

	@Value('${wi.query:}')
	String wiQuery
	
	@Value('${clm.projectArea:}')
	String projectName
	
	def currentItems
	
	public def getItems() {
		currentItems = clmWorkItemManagementService.getWorkItemsViaQuery(wiQuery)
		return currentItems
	}

	public String initialUrl() {
		def encoded = URLEncoder.encode(wiQuery, 'UTF-8')
		encoded = encoded.replace('+', '%20')
		String uri = this.clmGenericRestClient.clmUrl + "/ccm/rpt/repository/workitem?fields=" + encoded;
		return uri
	}

	public String getPageUrl() {
		def rel = currentItems.@rel
		if ("${rel}" != 'next') return null
		return currentItems.@href
	}

	public Object nextPage() {
		def rel = currentItems.@rel
		if ("${rel}" != 'next') return null
		currentItems = clmWorkItemManagementService.nextPage(currentItems.@href)
		return currentItems
	}

	public String getFilterName() {
		// TODO Auto-generated method stub
		return this.itemFilter;
	}

	public Date modifiedDate(Object item) {
		String sDate = "${item.modified.text()}"
		
		return new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", sDate);
	}

}
