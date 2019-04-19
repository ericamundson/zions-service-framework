package com.zions.rm.services.requirements

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
class ClmRequirementsFileManagementService {
	@Autowired
	ClmRequirementsManagementService clmRequirementsManagementService

	@Autowired
	IAttachments attachmentService
	
	@Autowired
	ICacheManagementService cacheManagementService

	public ClmRequirementsFileManagementService() {
		// TODO Auto-generated constructor stub
	}

	public def ensureRequirementFileAttachment(def itemData, String url, String altFilename) {
		def result = clmRequirementsManagementService.getContent(url)
		String contentDisp = "${result.headers.'Content-Disposition'}"
		String filename = null
		if (contentDisp != null && contentDisp != 'null') {
			filename = contentDisp.substring(contentDisp.indexOf('filename=')+10,contentDisp.indexOf('";'))
		}
		else {
			filename = altFilename
		}

		String attUrl
		if (filename != null) {
			def cacheLink = null
			def cacheWI = itemData.getCacheWI()
			if (cacheWI != null) {
				cacheLink = getCacheLink(cacheWI, filename)
			}
			if (cacheLink == null) { // Upload attachment to ADO
				def file = cacheManagementService.saveBinaryAsAttachment(result.data, filename, itemData.getCacheID())
				def attData = attachmentService.sendAttachment([file:file])
				attUrl = attData.url
				itemData.adoFileInfo.add([file: file, comment: "Added attachment ${filename}", url:attUrl])
			}
			else { // ADO attachment already exists in ADO.  Reference existing url.
				def encoded = URLEncoder.encode("${cacheLink.attributes.name}", 'utf-8')
				attUrl = "${cacheLink.url}?filename=$encoded"
			}
		}
		else {
			log.error("Error parsing filename for attachment.  Artifact ID: ${itemData.getIdentifier()}")
		}
		
		return [fileName: filename, url: attUrl]
	}
	
	private def getCacheLink(def cacheWI, String fileName) {
		def link = cacheWI.relations.find { rel ->
			def name = ""
			if (rel.attributes != null && rel.attributes.name != null) {
				name = "${rel.attributes.name}"
			}
			"${rel.rel}" == 'AttachedFile' && "${name}" == "${fileName}"
		}
		return link
	}
}
