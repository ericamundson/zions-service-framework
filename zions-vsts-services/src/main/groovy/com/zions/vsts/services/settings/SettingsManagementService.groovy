package com.zions.vsts.services.settings

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cache.MongoDBCacheManagementService
import com.zions.common.services.rest.IGenericRestClient
import groovy.json.JsonBuilder
import groovyx.net.http.ContentType

@Component
class SettingsManagementService {
	@Autowired(required=true)
	private IGenericRestClient genericRestClient;
	
	@Autowired
	ICacheManagementService cacheManagementService;

	
	def turnOffNotifications(collection) {
		def procs = cacheManagementService.getAllOfType('ExecutingProcess')
		String url = "${genericRestClient.getTfsUrl()}"
		if (!url.toLowerCase().endsWith('zionseto')) return
		def req = [defaultGroupDeliveryPreference:-1]
		String body = new JsonBuilder(req).toPrettyString()
		def result = genericRestClient.patch(
			contentType: ContentType.JSON,
			//requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/notification/Settings",
			body: body,
			query: ['api-version': '5.1-preview.1'],
			headers: ['Content-Type': 'application/json']
			
			)
		if (result != null) {
			String module = cacheManagementService.cacheModule
			String id = "${module}_running"
			cacheManagementService.saveToCache([ module: "${module}"], id, 'ExecutingProcess')
		}
		return result
	}
	
	def turnOnNotifications(collection) {
		String url = "${genericRestClient.getTfsUrl()}"
		if (!url.toLowerCase().endsWith('zionseto')) return
		String module = cacheManagementService.cacheModule
		String id = "${module}_running"
		cacheManagementService.deleteById(id)
		if (cacheManagementService instanceof MongoDBCacheManagementService) {
			MongoDBCacheManagementService cms = cacheManagementService
			Map wis = cms.getNoneModuleAllOfType('ExecutingProcess') 
			if (!wis.empty) return
		}
		def req = [defaultGroupDeliveryPreference:2]
		String body = new JsonBuilder(req).toPrettyString()
		def result = genericRestClient.patch(
			contentType: ContentType.JSON,
			//requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/notification/Settings",
			body: body,
			query: ['api-version': '5.1-preview.1'],
			headers: ['content-type': 'application/json']
			
			)
		return result
	}
}
