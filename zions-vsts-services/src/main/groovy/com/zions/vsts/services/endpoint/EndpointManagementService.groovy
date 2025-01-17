package com.zions.vsts.services.endpoint;

import org.apache.commons.lang.RandomStringUtils
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.tfs.rest.GenericRestClient;
import groovy.json.JsonBuilder
import groovyx.net.http.ContentType

@Component
public class EndpointManagementService {
	@Autowired
	private IGenericRestClient genericRestClient;
	
	public EndpointManagementService() {
		
	}
	
	public def ensureServiceEndpoint(String projectId, def endpointData, boolean grantAllPermission) {
		def query = ['api-version':'6.0-preview.4', excludeUrls:true]
		def endpoint = getServiceEndpoint('', projectId, endpointData.name)
		if (endpoint == null) {
			endpoint = createServiceEndpoint(endpointData)  
		} else {
			if (endpoint.type != endpointData.type) {
				deleteServiceEndpoint('', projectId, endpoint)
				endpoint = createServiceEndpoint(endpointData)
				
			} else {
				endpointData.id = endpoint.id
				updateServiceEndpoint(endpointData) 
			}
		}
		
		if (grantAllPermission) {
			def permissions = [ resource: [id: "${endpoint.id}", type:'endpoint', name: ''], pipelines: [], allPipelines: [authorized: true, authorizedBy: null, authorizedOn: null] ]
			def permsQuery = ['api-version':'5.1-preview.1', excludeUrls:true]
			String body = new JsonBuilder(permissions).toPrettyString()
			def result = genericRestClient.patch(
				contentType: ContentType.JSON,
				//requestContentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${projectId}/_apis/pipelines/pipelinePermissions/endpoint/${endpoint.id}",
				body: body,
				headers: [accept: 'application/json;api-version=5.1-preview.1;excludeUrls=true;enumsAsNumbers=true;msDateFormat=true;noArrayWrap=true', 'content-type': 'application/json']
				//query: permsQuery
				)
		}
		return endpoint
	}
	
	public def getServiceEndpointsByType(String collection, String projectId, String typeName) {
		def query = ['api-version': '7.1-preview.1', type:typeName]
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${projectId}/_apis/serviceendpoint/endpoints",
			query: query
			)
		return result
	}
	
	public def getServiceEndpoint(String collection, String projectId, String name) {
		def query = ['api-version': '6.0-preview.4', endpointNames:name]
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${projectId}/_apis/serviceendpoint/endpoints",
			query: query
			)
		def endpoint = null
		if (result.count > 0) {
			endpoint = result.value[0]
		}
		return endpoint
	}
	
	public def deleteServiceEndpoint(String collection, String projectId, def endpoint) {
		
		def query = ['api-version': '6.0-preview.4', projectIds:projectId]
		def result = genericRestClient.delete(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/_apis/serviceendpoint/endpoints/${endpoint.id}",
			query: query
			)
		return result
	}

	def createServiceEndpoint( def endpointData ) {
		def endpoint = genericRestClient.post(
			contentType: ContentType.JSON,
			requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/_apis/serviceendpoint/endpoints",
			body: endpointData,
			query: ['api-version':'6.0-preview.4', excludeUrls:true]
			)
		return endpoint
	}

	def updateServiceEndpoint( def endpointData ) {
		def endpoint = genericRestClient.put(
			contentType: ContentType.JSON,
			requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/_apis/serviceendpoint/endpoints/${endpointData.id}",
			body: endpointData,
			query: ['api-version':'6.0-preview.4', excludeUrls:true]
			)
		return endpoint
	}

	public def createGITServiceEndpoint(String collection, String projectId, String repoUrl, String bbUser, String bbPassword) {
		def epname = RandomStringUtils.random(5,true,false)
		epname = epname.toLowerCase()
		def query = ['api-version':'4.1-preview.2']
		def reqObj = [name: epname, type:'git', url: repoUrl, authorization: [parameters: [password: bbPassword, username: bbUser], scheme:'UsernamePassword']]
		def body = new JsonBuilder(reqObj).toString()
		ContentType t
		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			requestContentType: 'application/json',
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectId}/_apis/distributedtask/serviceendpoints",
			query: query,
			body: body
			)
		return result

	}

}
