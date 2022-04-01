package com.zions.sharepoint.services.page

import com.zions.common.services.rest.IGenericRestClient
import groovyx.net.http.ContentType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class QuickLaunchManagementService {
	
	@Autowired
	IGenericRestClient sharePointRestClient
	
	def cachedQuickLaunch 
	def cachedMaxKey
	
	def getQuickLaunch(String site, def nodeKey='1025') {
		def result = sharePointRestClient.get(
				contentType: ContentType.JSON,
				uri: "https://${sharePointRestClient.getZionsHost()}/$site/_api/navigation/MenuState",
				query: ['menuNodeKey': nodeKey]
				)
		if (result)
			return result
		else
			return null
	}
	
	def getNextKey(String site) {
		if (this.cachedMaxKey)
			return ++this.cachedMaxKey
			
		if (!cachedQuickLaunch) {
			getQuickLaunch(site)
		}
		
		// Find max quickLaunch key for this site
		def maxKey = 1025
		def nodes = cachedQuickLaunch.'**'.each { def node ->
			if (node.Key > maxKey)
				maxKey = node.Key
		}
		this.cachedMaxKey = ++maxKey
	}
	
	def saveQuickLaunch(String site, def nodeKey, def nodes) {
		def result = sharePointRestClient.post(
				contentType: ContentType.JSON,
				uri: "https://${sharePointRestClient.getZionsHost()}/$site/_api/navigation/MenuState",
				body: ["menuState": ["StartingNodeKey":"1025","Nodes":[["Key":"' + $NodeID + '", "Title":"' + $Title_en + '","SimpleUrl":"' + $NodeURL + '"]]]]
				)
		if (result)
			return result
		else
			return null
	}
	
	def addNode(String site, def pageTitle, def pageName, def startingNodeKey='1025') {
		def simpleUrl = "https://${sharePointRestClient.getZionsHost()}/sites/wikitest/SitePages/$pageName"
		def result = sharePointRestClient.post(
				contentType: ContentType.JSON,
				uri: "https://${sharePointRestClient.getZionsHost()}/$site/_api/navigation/MenuState",
				body: ['menuState': ['StartingNodeKey': startingNodeKey,
					   'Nodes': [['Key': getNextKey(site), 'Title': pageTitle, 'SimpleUrl': simpleUrl]]]]
				)
		if (result)
			return result
		else
			return null
	}

}
