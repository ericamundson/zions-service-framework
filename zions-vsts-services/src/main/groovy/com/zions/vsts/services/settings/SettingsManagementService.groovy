package com.zions.vsts.services.settings

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.common.services.rest.IGenericRestClient
import groovy.json.JsonBuilder
import groovyx.net.http.ContentType

@Component
class SettingsManagementService {
	@Autowired(required=true)
	private IGenericRestClient genericRestClient;

	
	def turnOffNotifications(collection) {
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
		return result
	}
	
	def turnOnNotifications(collection) {
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
