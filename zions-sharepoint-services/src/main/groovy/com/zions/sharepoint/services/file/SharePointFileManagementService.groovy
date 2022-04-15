package com.zions.sharepoint.services.file

import com.zions.common.services.rest.IGenericRestClient
import groovyx.net.http.ContentType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class SharePointFileManagementService {
	
	@Autowired
	IGenericRestClient sharePointRestClient
	def listDrives(def site) {
		def result = sharePointRestClient.get(
				contentType: ContentType.JSON,
				uri: "https://${sharePointRestClient.getGraphHost()}/beta/sites/${sharePointRestClient.getZionsHost()},$site/drives"
				)
		if (result)
			return result
		else
			return null
	}
	def searchFile(def site, def filename) {
		def result = sharePointRestClient.get(
				contentType: ContentType.JSON,
				uri: "$https://${sharePointRestClient.getGraphHost()}/beta/sites/${sharePointRestClient.getZionsHost()},$site/drive/root/search(q=${filename})"
				)
		if (result)
			return result
		else
			return null
	}
	// Upload new file
	def uploadFile(def site, def fileContent) {
		def result = sharePointRestClient.put(
				contentType: ContentType.BINARY,
				uri: "https://${sharePointRestClient.getGraphHost()}/beta/sites/${sharePointRestClient.getZionsHost()},$site/drive/items/root:/${fileContent.filename}:/content",
				body: fileContent.buf
				)
		if (result)
			return result
		else
			return null
	}
	// Upload existing file
	def uploadFile(def site, def itemid, def fileContent) {
		def result = sharePointRestClient.put(
				contentType: ContentType.BINARY,
				uri: "https://${sharePointRestClient.getGraphHost()}/beta/sites/${sharePointRestClient.getZionsHost()},$site/drive/items/${itemid}:/content",
				body: fileContent.buf
				)
		if (result)
			return result
		else
			return null
	}
	def ensureFile(def site, def fileContent) {
		// Check if file alreay exists
		def result = searchFile(site, fileContent.filename)
		if (result) 
			return uploadFile(site, result.itemid, fileContent)
		else
			return uploadFile(site, fileContent)
	}
}
