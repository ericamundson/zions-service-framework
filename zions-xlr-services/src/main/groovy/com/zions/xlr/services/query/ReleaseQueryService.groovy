package com.zions.xlr.services.query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import groovyx.net.http.ContentType
import groovy.json.JsonBuilder

import com.zions.xlr.services.rest.client.XlrGenericRestClient;

@Component
public class ReleaseQueryService {
	@Autowired
	XlrGenericRestClient xlrGenericRestClient
	
	
	def getReleases(def query) {
		def releases = []
		String body = new JsonBuilder(query).toPrettyString()
		int page = 0
		int pageSize = 25
		while (true) {
			def result = xlrGenericRestClient.post(
				requestContentType: ContentType.JSON,
				contentType: ContentType.JSON,
				uri:  "${xlrGenericRestClient.xlrUrl}/api/v1/releases/search",
				body: body,
				query: [page:page, resultsPerPage: pageSize]
				
			)
			releases.addAll(result)
			if (result.size() < pageSize) break
			page++
		}
		return releases
		
	}
	
	int getReleaseCount(def query) {
		
	}
}
