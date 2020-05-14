package com.zions.xlr.services.templates;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zions.xlr.services.rest.client.XlrGenericRestClient;
import com.zions.xlr.services.query.ReleaseQueryService
import com.zions.xlr.services.items.ReleaseItemService
import groovyx.net.http.ContentType

@Component
public class PhaseService {
	
	@Autowired
	XlrGenericRestClient xlrGenericRestClient
	
	@Autowired
	ReleaseItemService releaseItemService
	
	public PhaseService() {}
	
	def createTemplate(String folderId, def templateData) {
		def result = xlrGenericRestClient.post(
				requestContentType: ContentType.JSON,
				contentType: ContentType.JSON,
				uri:  "${xlrGenericRestClient.xlrUrl}/api/v1/templates",
				body: templateData,
				query: [folderId: folderId]
			)
	}
}
