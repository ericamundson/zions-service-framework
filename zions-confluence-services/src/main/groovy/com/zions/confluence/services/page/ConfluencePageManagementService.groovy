package com.zions.confluence.services.page

import com.zions.common.services.rest.IGenericRestClient
import groovyx.net.http.ContentType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ConfluencePageManagementService {
	
	@Autowired
	IGenericRestClient confluenceRestClient
	
	def getRootPage(String spaceKey) {
		def result = confluenceRestClient.get(
				contentType: ContentType.JSON,
				uri: "${confluenceRestClient.getUrl()}/rest/api/space/${spaceKey}/content?depth=root",
				query: ['expand': 'space,body.view,version,container']
				)
		if (result)
			return result.page.results[0]
		else
			return null
	}

	def getChildren(String pageId) {
		def result = confluenceRestClient.get(
				contentType: ContentType.JSON,
				uri: "${confluenceRestClient.getUrl()}/rest/api/content/${pageId}/child",
				query: ['expand': 'page.children.page.children.page']
				)
		if (result)
			return result.page.results
		else
			return null
	}
	
	def getPageContent(String pageId) {
		def result = confluenceRestClient.get(
				contentType: ContentType.JSON,
				uri: "${confluenceRestClient.getUrl()}/rest/api/content/${pageId}",
				query: ['expand': 'space,body.view,version,container,metadata.labels,children.attachment']
				)
		if (result)
			return result
	}
}
