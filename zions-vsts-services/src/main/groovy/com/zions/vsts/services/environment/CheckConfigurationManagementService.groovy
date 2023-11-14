package com.zions.vsts.services.environment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zions.common.services.rest.IGenericRestClient;
import com.zions.vsts.services.admin.member.MemberManagementService
import groovyx.net.http.ContentType

import groovy.util.logging.Slf4j;

@Component
@Slf4j
public class CheckConfigurationManagementService {
	@Autowired
	private IGenericRestClient genericRestClient;
	
	@Autowired
	MemberManagementService memberManagementService

	def getEnvCheckConfiguration(def col, def proj, def env) {
		
		  def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${col}/${proj.id}/_apis/pipelines/checks/configurations",
			query: [resourceType: 'environment', resourceId: env.id, 'api-version': '7.2-preview.1']
			)

		if (result && result.value && result.value.size() > 0) {
			for (def config in result.value) {
				if (config.resource.id as Integer == env.id as Integer) {
					return config
				}
			}
		}
		return null
	}
	
	def ensureEnvCheckConfiguration(def col, def proj, def env, String[] users, def settings = [blockedApprovers:[], executionOrder: 1, instructions: "", minRequiredApprovers: 1, requesterCannotBeApprover: true]) {
		def config = getEnvCheckConfiguration('', proj, env)
		def approvers = []
		for (def user in users) {
			def ident = memberManagementService.getIdentity('', user)
			if (ident && ident.size() > 0) {
				def identOut = [id: ident[0].id, displayName: ident[0].providerDisplayName]
				approvers.add(identOut)
			}
		}
		
		settings.approvers = approvers
		def configData = [ timeout: 43200, type: [id: '8c6f20a7-a545-4486-9777-f762fafe0d4d', name: 'Approval'], resource: [id: env.id, name: env.name, type: 'environment'], settings: settings]
		if (!config) {
		  def result = genericRestClient.post(
			contentType: ContentType.JSON,
			requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${col}/${proj.id}/_apis/pipelines/checks/configurations",
			query: ['api-version': '7.2-preview.1'],
			body: configData
			)
		  return result	
		} else {
			def result = genericRestClient.patch(
				contentType: ContentType.JSON,
				requestContentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${col}/${proj.id}/_apis/pipelines/checks/configurations/${config.id}",
				query: ['api-version': '7.2-preview.1'],
				body: configData
				)
			  return result
	
		}
		return config
		
	}
}
