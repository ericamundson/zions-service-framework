package com.zions.vsts.services.build

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.common.services.rest.IGenericRestClient
import groovyx.net.http.ContentType

import groovy.util.logging.Slf4j

@Component
@Slf4j
class PipelinePermissionsService {
	
	@Autowired
	private IGenericRestClient genericRestClient;

	def getPipelinePermission(String col, def proj, def resource) {
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${col}/${proj.id}/_apis/pipelines/pipelinePermissions/variablegroup/${resource.id}",
			query: ['api-version': '7.2-preview.1']
			)
		return result
	}
	
	def updatePipelinePermission(String col, def proj, def resource, def permissions) {
		
		Map pipeMap = [:]
		List pipesOut = []
		for (def pipe in permissions.pipelines) {
			int id = pipe.id
			if (!pipeMap.containsKey(id)) {
				pipeMap[id] = pipe
				pipesOut.add(pipe) 
			}
		}
		permissions.pipelines = pipesOut
		def result = genericRestClient.patch(
			contentType: ContentType.JSON,
			requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${col}/${proj.id}/_apis/pipelines/pipelinePermissions/variablegroup/${resource.id}",
			body: permissions,
			query: ['api-version': '7.2-preview.1']
			)

	}
}
