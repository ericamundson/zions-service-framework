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
	
	def getTemplates(String title, int depth = 0) {
		String rFolder = null
		String aTitle = null
		if (title.indexOf('/') != -1) {
			rFolder = title.substring(0, title.lastIndexOf('/'))
			aTitle = title.substring(title.lastIndexOf('/')+1)
		} else {
			aTitle = title
		}

		def templates = []
		int page = 0
		int pageSize = 25
		while (true) {
			def result = xlrGenericRestClient.get(
				//requestContentType: ContentType.JSON,
				contentType: ContentType.JSON,
				uri:  "${xlrGenericRestClient.xlrUrl}/api/v1/templates",
				query: [title: aTitle, depth: depth, page:page, resultsPerPage: pageSize]
				
			)
			templates.addAll(result)
			if (result.size() < pageSize) break
			page++
		}
		return templates

	}

	def getTemplate(String title, int depth = 0) {
		String rFolder = null
		String aTitle = null
		if (title.indexOf('/') != -1) {
			rFolder = title.substring(0, title.lastIndexOf('/'))
		    aTitle = title.substring(title.lastIndexOf('/')+1)
		} else {
			aTitle = title
		}

		def templates = []
		int page = 0
		int pageSize = 25
		while (true) {
			def result = xlrGenericRestClient.get(
				//requestContentType: ContentType.JSON,
				contentType: ContentType.JSON,
				uri:  "${xlrGenericRestClient.xlrUrl}/api/v1/templates",
				query: [title: aTitle, depth: depth, page:page, resultsPerPage: pageSize]
				
			)
			templates.addAll(result)
			if (result.size() < pageSize) break
			page++
		}
		def template = getActualTemplate( templates, rFolder)
		return template

	}
	
	def getActualTemplate(def templates, String rFolder) {
		if (rFolder == null && templates.size() > 0) {
			return templates[0]
		}
		def folder = getFolder(rFolder)
		if (folder != null&& templates.size() > 0) {
			String fId = folder.id
			for (def template in templates) {
				String tId = template.id
				
				if (tId.contains(fId)) {
					return template
				}
			}
		}
		return null
	}
	def getFolder(String path, int depth = 0) {
		def result = xlrGenericRestClient.get(
			//requestContentType: ContentType.JSON,
			contentType: ContentType.JSON,
			uri:  "${xlrGenericRestClient.xlrUrl}/api/v1/folders/find",
			query: [byPath: path, depth: depth]
			
		)
		return result

	}
	
	
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
	
	def getRelease(String id) {
		def result = xlrGenericRestClient.get(
			contentType: ContentType.JSON,
			uri:  "${xlrGenericRestClient.xlrUrl}/api/v1/releases/${id}"
			)
		return result
		
	}
	int getReleaseCount(def query) {
		
	}
}
