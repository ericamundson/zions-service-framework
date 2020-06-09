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
	
	def addPhase(String releaseId, def phaseData) {
		def result = xlrGenericRestClient.post(
				requestContentType: ContentType.JSON,
				contentType: ContentType.JSON,
				uri:  "${xlrGenericRestClient.xlrUrl}/api/v1/phases/${releaseId}/phase",
				body: phaseData
			)
	}
	
	def updatePhase(String phaseId, def phaseData) {
		def result = xlrGenericRestClient.put(
				requestContentType: ContentType.JSON,
				contentType: ContentType.JSON,
				uri:  "${xlrGenericRestClient.xlrUrl}/api/v1/phases/${phaseId}",
				body: phaseData
			)
	}
	
	def copyPhase(String releaseId, def phaseData, int position = 1) {
		def result = xlrGenericRestClient.post(
				requestContentType: ContentType.JSON,
				contentType: ContentType.JSON,
				uri:  "${xlrGenericRestClient.xlrUrl}/api/v1/phases/${releaseId}/copy",
				body: phaseData,
				query:[targetPosition: position]
			)
	}
	
	def deletePhase(String releaseId, def phaseData, int position = 1) {
		def result = xlrGenericRestClient.post(
				requestContentType: ContentType.JSON,
				contentType: ContentType.JSON,
				uri:  "${xlrGenericRestClient.xlrUrl}/api/v1/phases/${releaseId}/copy",
				body: phaseData,
				query:[targetPosition: position]
			)
	}

}
