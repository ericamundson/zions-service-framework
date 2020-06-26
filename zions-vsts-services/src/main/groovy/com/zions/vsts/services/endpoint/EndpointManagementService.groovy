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
	
	public def getServiceEndpoint(String collection, String projectId, String name) {
		def query = ['api-version': '4.1-preview.1', endpointNames:name]
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			requestContentType: 'application/json',
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectId}/_apis/serviceendpoint/endpoints",
			query: query
			)
		def endpoint = null
		if (result.count > 0) {
			endpoint = result.value[0]
		}
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
