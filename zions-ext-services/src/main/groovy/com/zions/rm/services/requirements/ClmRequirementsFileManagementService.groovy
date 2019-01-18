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

		String aurl = ritem.getFileHref()
		def result = clmRequirementsManagementService.getContent(aurl)
		String filename = ritem.getTitle()

		if (filename != null) {
			def file = cacheManagementService.saveBinaryAsAttachment(result.data, filename, id)
			item = [file: file, comment: "Added attachment ${filename}"]
		}

		return item
	}
}
