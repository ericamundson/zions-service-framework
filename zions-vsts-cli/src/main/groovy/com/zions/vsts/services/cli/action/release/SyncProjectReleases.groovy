package com.zions.vsts.services.cli.action.release;

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.build.BuildManagementService
import com.zions.vsts.services.release.ReleaseManagementService
import groovy.json.JsonSlurper


@Component
class SyncProjectReleases implements CliAction {
	ReleaseManagementService releaseManagementService
	
	@Autowired
	public SyncProjectReleases(ReleaseManagementService releaseService) {
		this.releaseManagementService = releaseService
	}

	@Override
	public def execute(ApplicationArguments data) {
		String collection = data.getOptionValues('tfs.collection')[0]
		String project = data.getOptionValues('tfs.project')[0]
		return releaseManagementService.ensureReleases(collection, project)
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.collection', 'tfs.token',  'tfs.project']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	
}