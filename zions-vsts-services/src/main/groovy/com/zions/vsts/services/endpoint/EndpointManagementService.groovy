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
	
	public def ensureServiceEndpoint(String projectId, def data, boolean grantAllPermission) {
		def query = ['api-version':'6.0-preview.4', excludeUrls:true]
		def endpoint = getServiceEndpoint('', projectId, data.name)
		if (endpoint == null) {
			endpoint = createServiceEndpoint(endpointData)  
		}
		
		if (grantAllPermission) {
			def permissions = [ resource: [id: "${endpoint.id}", type:'endpoint', name: ''], pipelines: [], allPipelines: [authorized: true, authorizedBy: null, authorizedOn: null] ]
			def permsQuery = ['api-version':'6.0-preview.4', excludeUrls:true]
			def result = genericRestClient.patch(
				contentType: ContentType.JSON,
				requestContentType: 'application/json',
				uri: "${genericRestClient.getTfsUrl()}/_apis/pipelines/pipelinePermissions/endpoint/${endpoint.id}",
				body: permissions,
				query: permsQuery
				)
		}
		return endpoint
	}
	
	public def getServiceEndpoint(String collection, String projectId, String name) {
		def query = ['api-version': '6.0-preview.4', endpointNames:name]
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			requestContentType: 'application/json',
			uri: "${genericRestClient.getTfsUrl()}/${projectId}/_apis/serviceendpoint/endpoints",
			query: query
			)
		def endpoint = null
		if (result.count > 0) {
			endpoint = result.value[0]
		}
		return endpoint
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
			uri: "${genericRestClient.getTfsUrl()}/_apis/serviceendpoint/endpoints",
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
