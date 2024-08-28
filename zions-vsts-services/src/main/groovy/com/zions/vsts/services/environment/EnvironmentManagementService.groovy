package com.zions.vsts.services.environment

import org.springframework.stereotype.Component

import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.build.BuildManagementService

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value
import groovyx.net.http.ContentType
import groovy.json.JsonOutput


import groovy.util.logging.Slf4j

@Component
@Slf4j
class EnvironmentManagementService {
	@Autowired
	private IGenericRestClient genericRestClient;
	
	@Autowired
	BuildManagementService buildManagementService
	
	@Autowired
	MemberManagementService memberManagementService

	
	def getEnvironment(String collection, def project, String name) {
		def result = null
		try {
		  result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/pipelines/environments",
			query: [name: name, 'api-version': '7.2-preview.1'],
			)
		} catch (e) {}
		if (result && result.value && result.value.size() > 0) {
			return result.value[0]
		}
		return null
	}

	

	def ensureEnvironment(def collection, def project, String name, String description = null) {
		def env = getEnvironment('', project, name)
		
		if (env == null) {
			//deleteEnvironment('', project, name, env)
		
			def body = [name: name]
			if (description != null) {
				body.description = description
			}
			def result = genericRestClient.post(
				contentType: ContentType.JSON,
				requestContentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/pipelines/environments",
				body: body,
				query: ['api-version': '7.2-preview.1']
				)
			// Permissions
			String url = genericRestClient.getTfsUrl()
			String org = 'eto-dev'
			if (!url.contains('eto-dev')) {
				org = 'ZionsETO'
			}
			def codeops = memberManagementService.getGroup('', "[${project.name}]\\Project Valid Users")
			if (codeops) {
				def inperms = [userId: codeops.originId, roleName: 'Administrator']
				def inp = json(inperms)
				def sec = genericRestClient.put(
					contentType: ContentType.JSON,
					//requestContentType: ContentType.JSON,
					uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/securityroles/scopes/distributedtask.environmentreferencerole/roleassignments/resources/${project.id}_${result.id}/${codeops.originId}",
					body: inp,
					query: ['api-version': '7.2-preview.1']
					)
				println sec
			}
			return result
	
		}
		return env
		
	}
	
	String json(def data) {
		String o = JsonOutput.toJson(data)
		return JsonOutput.prettyPrint(o)
	}

	
	def deleteEnvironment(def collection, def project, String name, def env) {
			def result = genericRestClient.delete(
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/pipelines/environments/${project.id}",
				query: ['api-version': '7.2-preview.1']
				)
			return result
	}
	
	def getDeploymentRecords(String collection, def project, String name) {
		def env = getEnvironment(collection, project, name)
		List executions = []
		int top = 100
		def result = null
		result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/pipelines/environments/${env.id}/environmentdeploymentrecords",
			headers: [Accept: 'application/json'],
			query: ['api-version': '7.2-preview.1'],
			withHeader: true					
			)
		while (true) {
			executions.addAll(result.data.'value')
			if (result.headers.'X-MS-ContinuationToken') {
				result = genericRestClient.get(
					contentType: ContentType.JSON,
					uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/pipelines/environments/${env.id}/environmentdeploymentrecords",
					headers: [Accept: 'application/json'],
					query: ['api-version': '7.2-preview.1', continuationToken: result.headers.'X-MS-ContinuationToken'],
					withHeader: true					
					)
			} else {
				break;
			}
		}
		return executions
	}

}
