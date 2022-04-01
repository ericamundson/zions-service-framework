package com.zions.confluence.services.page

import com.zions.common.services.rest.IGenericRestClient
import groovyx.net.http.ContentType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ConfluenceFileManagementService {
	
	@Autowired
	IGenericRestClient confluenceRestClient
	
	def getFileContent(String source) {
		// Pull query parameters off source
		def filename
		def result
		if (source.indexOf('?') > 0) {
			def link = source.split('\\?')
			filename = parseFilename(link[0])
			def queryParms = [:]
			link[1].split('\\&').each { parm ->
				def mapEntry = parm.split('\\=')
				queryParms.put(mapEntry[0], mapEntry[1])
			}
			result = confluenceRestClient.get(
					contentType: ContentType.BINARY,
					uri: "${confluenceRestClient.getUrl()}${link[0]}",
					query: queryParms
					)
		} else {
			filename = parseFilename(source)
			result = confluenceRestClient.get(
				contentType: ContentType.BINARY,
				uri: "${confluenceRestClient.getUrl()}${source}"
				)
		}
		if (result)
			return [buf: result.buf, size: result.count, filename: filename]
		else
			return null
	}
	def parseFilename(String source) {
		String filename = source.substring(source.lastIndexOf('/')+1,source.length())
		if (filename.indexOf('.') == -1)
			filename += '.png'
		return filename
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
				query: ['expand': 'space,body.view,version,container']
				)
		if (result)
			return result
	}
}
