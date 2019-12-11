package com.zions.clm.services.cli.action.work.query

import com.zions.clm.services.ccm.workitem.CcmWorkManagementService
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
	CcmWorkManagementService ccmWorkManagementService
	
	@Value('${clm.query.names:}')
	String[] clmQueryNames
	
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
		
		List<String> queryNames = []
		queryNames.addAll(Arrays.asList(clmQueryNames))
		String pageId = "${page}"
		ccmWorkManagementService.multiQuery(queryNames, projectName)
		new CacheInterceptor() {}.provideCaching(clmWorkItemManagementService, pageId, currentTimestamp, QueryTracking) {
			currentItems = ccmWorkManagementService.multiNext()
		}
		return currentItems
	}

	public String initialUrl() {
//		def encoded = URLEncoder.encode(wiQuery, 'UTF-8')
//		encoded = encoded.replace('+', '%20')
		String uri = ccmWorkManagementService.multiPageUrl();
		return uri
	}

	public String getPageUrl() {
//		def rel = currentItems.@rel
//		if ("${rel}" != 'next') return null
		//page++
		return ccmWorkManagementService.multiPageUrl();
	}

	public Object nextPage() {
//		def rel = currentItems.@rel
//		if ("${rel}" != 'next') return null
		page++
		String pageId = "${page}"
		new CacheInterceptor() {}.provideCaching(clmWorkItemManagementService, pageId, currentTimestamp, QueryTracking) {
			//String url = "${currentItems.@href}"
			//currentItems = clmWorkItemManagementService.nextPage(url)
			currentItems = ccmWorkManagementService.multiNext()
		}
		return currentItems
	}

	public String getFilterName() {
		return this.itemFilter;
	}

	public boolean isModified(Object item) {
		String key = "${item.id}"
		def cacheWI = cacheManagementService.getFromCache(key, ICacheManagementService.WI_DATA)
		if (!cacheWI) return true
		String sDate = "${item.modified}"
		Date clmDate = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sDate);
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
