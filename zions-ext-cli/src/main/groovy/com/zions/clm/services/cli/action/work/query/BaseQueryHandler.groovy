package com.zions.clm.services.cli.action.work.query

import com.zions.clm.services.rtc.project.workitems.ClmWorkItemManagementService
import com.zions.clm.services.rtc.project.workitems.QueryTracking
import com.zions.common.services.cache.CacheInterceptorService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cacheaspect.CacheInterceptor
import com.zions.common.services.logging.FlowInterceptor
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.restart.Checkpoint
import com.zions.common.services.restart.ICheckpointManagementService
import com.zions.common.services.restart.IQueryHandler
import com.zions.qm.services.test.ClmTestManagementService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

class BaseQueryHandler implements IQueryHandler {
	
	@Autowired
	ClmWorkItemManagementService clmWorkItemManagementService
	
	@Autowired
	ICacheManagementService cacheManagementService
	
	@Value('${wi.filter:allFilter}')
	private String itemFilter

	@Autowired
	IGenericRestClient clmGenericRestClient

	@Value('${wi.query:}')
	String wiQuery
	
	@Value('${clm.projectArea:}')
	String projectName
	
	def currentItems
	
	Date currentTimestamp = new Date()
	
	int page = 0
	
	public def getItems() {
		def cp = cacheManagementService.getFromCache('query', 'QueryStart')
		page=0
		if (cp) {
			currentTimestamp = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", cp.timestamp)
			
		}
		String pageId = "${page}"
		new CacheInterceptor() {}.provideCaching(clmWorkItemManagementService, pageId, currentTimestamp, QueryTracking) {
			currentItems = clmWorkItemManagementService.getWorkItemsViaQuery(wiQuery)
		}
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
		page++
		String pageId = "${page}"
		new CacheInterceptor() {}.provideCaching(clmWorkItemManagementService, pageId, currentTimestamp, QueryTracking) {
			String url = "${currentItems.@href}"
			currentItems = clmWorkItemManagementService.nextPage(url)
		}
		return currentItems
	}

	public String getFilterName() {
		return this.itemFilter;
	}

	public boolean isModified(Object item) {
		String key = "${item.id.text()}"
		def cacheWI = cacheManagementService.getFromCache(key, ICacheManagementService.WI_DATA)
		if (!cacheWI) return true
		String sDate = "${item.modified.text()}"
		Date clmDate = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", sDate);
		String modified = cacheWI.fields['System.ChangedDate']
		if (modified != null && modified.length()> 0) {
			if (modified.lastIndexOf('.') > -1) {
				modified = modified.substring(0, modified.lastIndexOf('.'))
			} else {
				modified = modified.replace('Z', '')
			}
			modified = modified + ".999Z"
			Date modDate = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", modified)
			return clmDate.time >= modDate.time;
		} 
		return true;
	}


}
