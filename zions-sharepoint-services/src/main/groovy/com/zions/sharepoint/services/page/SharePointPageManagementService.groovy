package com.zions.sharepoint.services.page

import com.zions.common.services.rest.IGenericRestClient
import groovyx.net.http.ContentType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class SharePointPageManagementService {
	
	@Autowired
	IGenericRestClient sharePointRestClient
	
	def getSiteTitle(String site) {
		def result = sharePointRestClient.get(
				contentType: ContentType.JSON,
				uri: "${sharePointRestClient.getUrl()}/$site/_api/web/title"
				)
		if (result)
			return result
		else
			return null
	}
	def createPage(String site, String pageName, String pageTitle, String html) {
		def webParts = []
		webParts.add(['type': 'rte', 'data': ['innerHTML': html.trim().replace('\r\n','').replaceAll(">[\\s\r\n]*<", "><")]])
		def result = sharePointRestClient.post(
				contentType: ContentType.JSON,
				uri: "${sharePointRestClient.getUrl()}/beta/sites/zionsbancorporation.sharepoint.com,$site/pages",
				body: [['name': pageName, 'title': pageTitle, 'publishingState': ['level': 'published', 'versionId': '0.1']],
					   ['webParts': webParts]]
				)
		if (result)
			return result
		else
			return null
	}
	
	

}
