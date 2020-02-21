package com.zions.vsts.services.cli.action.release;

import java.util.Map
import com.zions.common.services.cli.action.CliAction

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.xlr.services.maintenance.MaintenanceService
import com.zions.xlr.services.query.ReleaseQueryService
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper


@Component
class CleanupXLRReleases implements CliAction {
	
	@Autowired
	MaintenanceService maintenanceService
	
	@Autowired
	ReleaseQueryService releaseQueryService
	
	@Value('${release.folderId:}')
	String releaseFolderId
		
	@Value('${release.title:}')
	String releaseTitle
	
	@Value('${release.title.filter:}')
	String releaseTitleFitler

	public def execute(ApplicationArguments data) {
		//getCurrent Release
		def query = [ title: releaseTitle]
		def releases = releaseQueryService.getReleases(query)
		if (releases.size() > 0) {
			maintenanceService.cleanupOrphanedReleases(releases[0], releaseFolderId, releaseTitleFitler)
		}
	}

	public Object validate(ApplicationArguments args) throws Exception {
		return true
	}
	
}