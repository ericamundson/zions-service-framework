package com.zions.rm.services.cli.action.requirements

import com.zions.common.services.cache.CacheInterceptorService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cacheaspect.CacheInterceptor
import com.zions.common.services.logging.FlowInterceptor
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.restart.Checkpoint
import com.zions.common.services.restart.ICheckpointManagementService
import com.zions.common.services.restart.IQueryHandler
import com.zions.rm.services.requirements.ClmRequirementsManagementService
import com.zions.rm.services.requirements.RequirementQueryData
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

//v070873 copied from BaseQueryHandler for other methods
//I'm confused as to the duplication present in this class when accounting for TranslateRmBaseArtifacts.
//I am kind of thinking TranslateRmBaseArtifacts is like the view,
//ClmRequirementsManagementService is like the model, and this is the controller
//still this is how I think it goes down
@Slf4j
class BaseQueryHandler implements IQueryHandler {
	
	@Autowired
	ClmRequirementsManagementService clmRequirementsManagementService
	
	@Autowired
	ICacheManagementService cacheManagementService
	
//	@Value('${wi.filter:allFilter}')
//	private String itemFilter

	@Autowired
	IGenericRestClient rmGenericRestClient
//
	@Value('${clm.projectAreaUri:}')
	String projectURI
	@Value('${oslc.namespaces:}')
	String oslcNs
	@Value('${oslc.select:}')
	String oslcSelect
	@Value('${oslc.where:}')
	String oslcWhere
	@Value('${rm.filter:}')
	String rmFilter
	@Value('${clm.pageSize}')
	String clmPageSize
	
	def currentItems
	
	Date currentTimestamp = new Date()
	
	int page = 0
	
	//retrieve items from the cache, or set up a cache intercepter for them
	//what does that do?
	public def getItems() {
		def cp = cacheManagementService.getFromCache('query', 'QueryStart')
		page=0
		if (cp) {
			currentTimestamp = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", cp.timestamp)
			log.debug("Timestamp from cachepage: ${currentTimestamp}")
		} 
//		else {
//			cacheManagementService.saveToCache([timestamp: currentTimestamp.format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")], 'query', 'QueryStart')
//			log.debug("Creating first QueryStart cache for RM")
//		}
		String pageId = "${page}"
		log.debug("getItems for page ${pageId} at timestamp ${currentTimestamp}")
		new CacheInterceptor() {}.provideCaching(clmRequirementsManagementService, pageId, currentTimestamp, RequirementQueryData) {
			currentItems = clmRequirementsManagementService.queryForArtifacts(projectURI, oslcNs, oslcSelect, oslcWhere)
		}
		return currentItems
	}

	public String initialUrl() {
		String rmquery = oslcNs + oslcSelect + oslcWhere.replace('zpath',this.rmGenericRestClient.clmUrl);
		def encoded = URLEncoder.encode(rmquery, 'UTF-8')
		encoded = encoded.replace('<','%3C').replace('>', '%3E')
		String uri = this.rmGenericRestClient.clmUrl + "/rm/views?oslc.query=&projectURL=" + this.rmGenericRestClient.clmUrl + "/rm/process/project-areas/" + projectURI + encoded + "&oslc.pageSize=${clmPageSize}"
		return uri
	}

	public String getPageUrl() {
		String nextUrl = "${currentItems.ResponseInfo.nextPage.@'rdf:resource'}"
		if (nextUrl == '') return null
		return nextUrl
	}

	//avoiding code duplication by referencing getPageUrl
	//storing it because I have some fear that invoking getPageUrl twice might double skip page
	//probably unfounded but it can't hurt to save it
	public Object nextPage() {
		String nextUrl = this.getPageUrl()
		if (nextUrl == null) return null
		page++
		String pageId = "${page}"
		log.debug("Retrieving next RM page: ${page}")
		new CacheInterceptor() {}.provideCaching(clmRequirementsManagementService, pageId, currentTimestamp, RequirementQueryData) {
			currentItems = clmRequirementsManagementService.nextPage(nextUrl)
		}
		log.debug("Returning next RM page: ${page}")
		return currentItems
	}

	public String getFilterName() {
		// TODO Auto-generated method stub
		return this.rmFilter;
	}

	public Date modifiedDate(Object item) {
		String sDate = "${item.modified.text()}"
		// might be something more like: rmItemData.Requirement.modified
		return new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", sDate);
	}

}
