package com.zions.rm.services.requirements

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
	ICacheManagementService cacheManagementService
	
	@Autowired
	RequirementsMappingManagementService requirementsMappingManagementService

	public ClmRequirementsFileManagementService() {
		// TODO Auto-generated constructor stub
	}

	public def cacheRequirementFile(def ritem) {
		def item
		String type = ritem.getTfsWorkitemType()
		String id = "${ritem.getID()}-${type}"

		def result = clmRequirementsManagementService.getContent(ritem.getFileHref())
		String contentDisp = "${result.headers.'Content-Disposition'}"
		def start = contentDisp.indexOf('filename=')+10
		def end = contentDisp.indexOf('";')
		String filename = null
		if (start > 0 && end > 0 && end > start) {
				filename = contentDisp.substring(start,end)
		}

		if (filename != null) {
			def file = cacheManagementService.saveBinaryAsAttachment(result.data, filename, id)
			item = [file: file, comment: "Added attachment ${filename}"]
		}

		return item
	}
}
