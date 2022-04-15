package com.zions.vsts.services.security

import com.zions.common.services.rest.IGenericRestClient
import groovy.json.JsonBuilder
import groovy.util.logging.Log
import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
@Slf4j
class SecurityManagementService {
	
	@Autowired
	IGenericRestClient genericRestClient
	
	def namespaceCache = [:]
	
	def getNamespaces(String collection) {
		if (namespaceCache[collection])
			return namespaceCache[collection]
			
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}//_apis/securitynamespaces",
				query: ['api-version': '6.0']
			)
		
		// Cache the namespaces for this collection
		if (result) {
			namespaceCache.put(collection, result.value)				
			return result.value
		}
		else
			return null
	}
	
	def getNamespace(String collection, String name) {
		def namespaces = getNamespaces(collection)
		
		return namespaces.find { ns -> 
			ns.name == name }
	}
	
	def queryAcls(String collection, String nsId, String descriptors) {
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}//_apis/accesscontrollists/$nsId",
			query: ['descriptors': descriptors, 'api-version': '6.0']
		)
		if (result)
			return result.value
		else
			return null
	}
	
}
