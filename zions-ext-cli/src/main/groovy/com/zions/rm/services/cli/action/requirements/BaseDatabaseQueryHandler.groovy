package com.zions.rm.services.cli.action.requirements

import com.zions.common.services.cache.CacheInterceptorService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cacheaspect.CacheInterceptor
import com.zions.common.services.cacheaspect.CacheWData
import com.zions.common.services.db.DatabaseQueryService
import com.zions.common.services.db.IDatabaseQueryService
import com.zions.common.services.logging.FlowInterceptor
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.restart.Checkpoint
import com.zions.common.services.restart.ICheckpointManagementService
import com.zions.common.services.restart.IQueryHandler
import com.zions.rm.services.requirements.ClmRequirementsManagementService
import com.zions.rm.services.requirements.DataWarehouseQueryData
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

//v070873 copied from BaseQueryHandler for other methods
//I'm confused as to the duplication present in this class when accounting for TranslateRmBaseArtifacts.
//I am kind of thinking TranslateRmBaseArtifacts is like the view,
//ClmRequirementsManagementService is like the model, and this is the controller
//still this is how I think it goes down
@Slf4j
class BaseDatabaseQueryHandler implements IQueryHandler {
	
	@Autowired
	ClmRequirementsManagementService clmRequirementsManagementService
	
	@Autowired
	IDatabaseQueryService databaseQueryService
	
	@Autowired
	ICacheManagementService cacheManagementService
	
//	@Value('${wi.filter:allFilter}')
//	private String itemFilter

	@Autowired
	IGenericRestClient rmGenericRestClient
//
	@Value('${rm.filter:}')
	String rmFilter
	
	def currentItems
	
	Date currentTimestamp = new Date()
	
	int page = 0
	
	//retrieves items from oracle
	public def getItems() {
		def cp = cacheManagementService.getFromCache('query', 'QueryStart')
		def deltaCacheDate = cacheManagementService.getFromCache('last', 'QueryDelta')
		def queryEndDate
		page=0
		if (cp) {
			currentTimestamp = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", cp.timestamp)
			queryEndDate = currentTimestamp;
			log.debug("QueryStart retrieved from cache: ${currentTimestamp}")
		} 
		if (deltaCacheDate) {
			currentTimestamp = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", deltaCacheDate.timestamp)
			log.debug("Delta querystart in cache, running delta job from QueryStart to ${currentTimestamp}")
		}
		log.info("Performing delta on update from original QueryStart date to ${queryEndDate}")
//		else {
//			cacheManagementService.saveToCache([timestamp: currentTimestamp.format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")], 'query', 'QueryStart')
//			log.debug("Creating first QueryStart cache for RM")
//		}
		String pageId = "${page}"
		log.debug("getItems for page ${pageId} with cache pages taken ${currentTimestamp}")
		new CacheInterceptor() {}.provideCaching(clmRequirementsManagementService, pageId, currentTimestamp, DataWarehouseQueryData) {
			currentItems = clmRequirementsManagementService.queryDatawarehouseSource(queryEndDate)
		}
		return currentItems
	}

	//feels weird to just call like this
	public String initialUrl() {
		return clmRequirementsManagementService.initialUrlDb(currentTimestamp)
	}

	public String getPageUrl() {
		return clmRequirementsManagementService.pageUrlDb()
	}

	//see it's caching the data warehouse stuff page by page, I think
	public Object nextPage() {
//		String nextUrl = this.getPageUrl()
//		if (nextUrl == null) return null
		page++
		String pageId = "${page}"
		log.debug("Retrieving next DB page: ${page}")
		new CacheInterceptor() {}.provideCaching(clmRequirementsManagementService, pageId, currentTimestamp, DataWarehouseQueryData) {
			currentItems = clmRequirementsManagementService.nextPageDb()
		}
		log.debug("Returning next DB page: ${page}")
		return currentItems
	}

	public String getFilterName() {
		
		return this.rmFilter;
	}

	public Date modifiedDate(Object item) {
		String sDate = "${item.modified.text()}"
		// might be something more like: rmItemData.Requirement.modified
		return new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", sDate);
	}

	public boolean isModified(Object item) {
		return true;
	}

}