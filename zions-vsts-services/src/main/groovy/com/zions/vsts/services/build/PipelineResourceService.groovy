package com.zions.vsts.services.build

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.common.services.rest.IGenericRestClient
import groovyx.net.http.ContentType
import groovy.json.JsonOutput

import groovy.util.logging.Slf4j

@Component
@Slf4j
class PipelineResourceService {
	@Autowired
	private IGenericRestClient genericRestClient;
	
	def getResourceReference(String col, def project, int buildDefId) {
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${col}/${project.id}//_apis/build/definitions/${buildDefId}/resources",
			query: ['api-version': '7.2-preview.1'],
			)
		return result
	}
}
