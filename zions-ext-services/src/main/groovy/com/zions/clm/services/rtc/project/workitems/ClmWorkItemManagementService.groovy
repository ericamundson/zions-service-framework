package com.zions.clm.services.rtc.project.workitems;

import org.springframework.beans.factory.annotation.Autowired;
import com.zions.common.services.cacheaspect.CacheInterceptor
import org.springframework.stereotype.Component;

import com.zions.clm.services.rest.ClmGenericRestClient;
import com.zions.common.services.cache.CacheInterceptorService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cacheaspect.Cache
import com.zions.common.services.link.LinkInfo
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.restart.Checkpoint
import com.zions.common.services.restart.ICheckpointManagementService

import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil
import groovyx.net.http.ContentType

/**
 * Handles RTC work item queries which provide DTO return of XML results
 * 
 * @see groovy.util.slurpersupport.GPathResult
 * 
 * @author z091182
 *
 */
@Component
@Slf4j
public class ClmWorkItemManagementService {

	@Autowired
	IGenericRestClient clmGenericRestClient
	
	@Autowired(required=false)
	ICheckpointManagementService checkpointManagementService
	
	@Autowired(required=false)
	ICacheManagementService cacheManagementService
	

	public ClmWorkItemManagementService() {
		
	}
	
	def flushQueries(String query) {
		Date ts = new Date()
		cacheManagementService.saveToCache([timestamp: ts.format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")], 'query', 'QueryStart')
		int page = 0
		def currentItems
		new CacheInterceptor() {}.provideCaching(this, "${page}", ts, QueryTracking, ['getWorkItemsViaQuery','nextPage']) {
			currentItems = this.getWorkItemsViaQuery(query)
		}
		while (true) {
			def rel = currentItems.@rel
			if ("${rel}" != 'next') break
			page++
			new CacheInterceptor() {}.provideCaching(this, "${page}", ts, QueryTracking) {
				String url = "${currentItems.@href}"
				currentItems = this.nextPage(url)
			}
	
		}
	}
	
	def getContent(String uri) {
		def result = clmGenericRestClient.get(
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

	def getWorkItemHistory(int id) {
		def uri = "${this.clmGenericRestClient.clmUrl}/ccm/service/com.ibm.team.workitem.common.internal.rest.IWorkItemRestService/workItemDTO2"
		def query = [id: id, includeAttributes: false, includeLinks: false, includeApprovals: false, includeHistory: true, includeLinkHistory: true]
		def result = clmGenericRestClient.get(
			contentType: ContentType.JSON,
			uri: uri,
			query: query,
			headers: [accept: 'text/json', 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8'] );
		return result
	}
	
	public def getWorkItemsViaQuery(String query) {
		//def query = "workitem/workItem[projectArea/name='${project}']/(id)"
		def encoded = URLEncoder.encode(query, 'UTF-8')
		encoded = encoded.replace('+', '%20')
		String uri = this.clmGenericRestClient.clmUrl + "/ccm/rpt/repository/workitem?fields=" + encoded;
		def result = clmGenericRestClient.get(
				uri: uri,
				headers: [Accept: 'text/xml'] );
			//XmlUtil u
//		String xml = XmlUtil.serialize(result)
//		File out = new File('tools_wi.xml')
//		def o = out.newDataOutputStream()
//		o << new groovy.xml.StreamingMarkupBuilder().bindNode(result) as String
//		o.close()
		return result
	}
	
	public def nextPage(String url) {
		def result = clmGenericRestClient.get(
			uri: url,
			headers: [Accept: 'text/xml'] );
		return result
	}
}
