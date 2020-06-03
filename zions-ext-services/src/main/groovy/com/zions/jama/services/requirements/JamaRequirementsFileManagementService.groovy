package com.zions.jama.services.requirements

import com.zions.common.services.attachments.IAttachments
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.qm.services.test.ClmTestManagementService
import com.zions.qm.services.test.TestMappingManagementService

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
@Slf4j
class JamaRequirementsFileManagementService {
	@Autowired
	JamaRequirementsManagementService jamaRequirementsManagementService

	@Autowired
	IAttachments attachmentService
	
	@Autowired
	ICacheManagementService cacheManagementService

	public JamaRequirementsFileManagementService() {
		// Do nothing
	}

	public def ensureRequirementFileAttachment(def itemData, String url) {

		String attUrl
		def cacheLink = null
		def cacheWI = cacheManagementService.getFromCache("${itemData.getCacheID()}", ICacheManagementService.WI_DATA)
		def iStart = url.lastIndexOf('/')+1
		String filename = url.substring(iStart,url.length())
		if (cacheWI != null) {
			cacheLink = getCacheLink(cacheWI, filename)
		}
		if (cacheLink == null) { // Upload attachment to ADO
			def result = jamaRequirementsManagementService.getContent(url)
			if (result.headers['Content-Type'] != 'application/octet-stream') {
				log.error('Error retrieving embedded image')
				return null
			}
			ByteArrayInputStream content = result.data
			def attData = attachmentService.sendAttachment(content.bytes, filename)
			if (attData) {
				attUrl = attData.url
				itemData.adoFileInfo.add([comment: "Added attachment ${filename}", url:attUrl])
			} else {
//					if (checkpointManagementService != null) {
//						checkpointManagementService.addLogentry("File upload attempt failed for Artifact ID: ${itemData.getIdentifier()} Filename: ${filename}")
//					}
				return null
			}
		}
		else { // ADO attachment already exists in ADO.  Reference existing url.
			def encoded = URLEncoder.encode("${cacheLink.attributes.name}", 'utf-8')
			attUrl = "${cacheLink.url}?filename=$encoded"
		}
		
		return [fileName: filename, url: attUrl]
	}
	public def ensureMultipleFileAttachments(def itemData, def attIds) {
		attIds.each { id -> 
			String attUrl
			def cacheLink = null
			def attResult = jamaRequirementsManagementService.getAttachment(id)
			String filename = "${attResult.data.fileName}"
			def cacheWI = cacheManagementService.getFromCache("${itemData.getCacheID()}", ICacheManagementService.WI_DATA)
			if (cacheWI != null) {
				cacheLink = getCacheLink(cacheWI, filename)
			}
			if (cacheLink == null) { // Upload attachment to ADO
				def result = jamaRequirementsManagementService.getContent(id)
				ByteArrayInputStream content = result.data
				def attData = attachmentService.sendAttachment(content.bytes, filename)
				if (attData) {
					attUrl = attData.url
					itemData.adoFileInfo.add([comment: "Added attachment ${filename}", url:attUrl])
				} else {
	//					if (checkpointManagementService != null) {
	//						checkpointManagementService.addLogentry("File upload attempt failed for Artifact ID: ${itemData.getIdentifier()} Filename: ${filename}")
	//					}
					return null
				}
			}
			
		}
	}
	private def getCacheLink(def cacheWI, String fileName) {
		if (cacheWI.relations) {
			def link = cacheWI.relations.find { rel ->
				def name = ""
				if (rel.attributes != null && rel.attributes.name != null) {
					name = "${rel.attributes.name}"
				}
				"${rel.rel}" == 'AttachedFile' && "${name}" == "${fileName}"
			}
			return link
		}
		else return null
	}
}
