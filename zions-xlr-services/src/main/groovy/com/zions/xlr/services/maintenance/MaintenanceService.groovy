package com.zions.xlr.services.maintenance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zions.xlr.services.rest.client.XlrGenericRestClient;
import com.zions.xlr.services.query.ReleaseQueryService
import com.zions.xlr.services.items.ReleaseItemService

@Component
public class MaintenanceService {
	
	@Autowired
	ReleaseQueryService queryService
	
	@Autowired
	ReleaseItemService releaseItemService
	
	public MaintenanceService() {}
	
	def cleanupOrphanedReleases(def currentRelease, String folderId, String titleFilter = null) {
		def query = [ parentId: folderId, inProgress: true]
		if (titleFilter) {
			query.title = titleFilter
		}
		String newReleaseId = "${currentRelease.id}"
		String newReleaseTitle = "${currentRelease.title}"
		String templateId = "${currentRelease.originTemplateId}"
		def releases = queryService.getReleases(query)
		
		releases.each { release -> 
			String rTemplateId = "${release.originTemplateId}"
			String rId = "${release.id}"
			if (rTemplateId == templateId && rId != newReleaseId) {
				releaseItemService.abortRelease(rId, newReleaseTitle)
			}
		}
	}

}
