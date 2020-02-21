package com.zions.xlr.services.items;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import groovyx.net.http.ContentType
import groovy.json.JsonBuilder

import com.zions.xlr.services.rest.client.XlrGenericRestClient;
import com.zions.xlr.services.query.ReleaseQueryService

@Component
public class ReleaseItemService {
	@Autowired
	XlrGenericRestClient xlrGenericRestClient
	
	@Autowired
	ReleaseQueryService queryService
	
	def deleteRelease(String releaseId) {
		def result = xlrGenericRestClient.delete(
			uri:  "${xlrGenericRestClient.xlrUrl}/api/v1/releases/${releaseId}"
		)

	}

	def abortRelease(String releaseId, String newReleaseTitle = null) {
		def message = [abortComment: "A newer release has been started."]
		if (newReleaseTitle) {
			message.abortComment = "Newer release '${newReleaseTitle}' has been started."
		}
		String body = new JsonBuilder(message).toPrettyString()
		def result = xlrGenericRestClient.post(
			requestContentType: ContentType.JSON,
			contentType: ContentType.JSON,
			uri:  "${xlrGenericRestClient.xlrUrl}/api/v1/releases/${releaseId}/abort",
			body: body
		)

	}
}
