package com.zions.vsts.services.cli.action.build;

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.build.BuildManagementService
import com.zions.vsts.services.permissions.PermissionsManagementService
import groovy.json.JsonSlurper


@Component
class SyncProjectBuilds implements CliAction {
	BuildManagementService buildManagementService
	PermissionsManagementService permissionsManagementService
	
	@Autowired
	public SyncProjectBuilds(BuildManagementService buildService,
		PermissionsManagementService permissionsManagementService) {
		this.buildManagementService = buildService
		this.permissionsManagementService = permissionsManagementService
	}

	@Override
	public def execute(ApplicationArguments data) {
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		String project = data.getOptionValues('tfs.project')[0]
		String template = data.getOptionValues('grant.template')[0]
		buildManagementService.ensureBuilds(collection, project)
		permissionsManagementService.updateBuilderPermissions(collection, project, template)
		return null
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.token',  'tfs.project', 'grant.template']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	
}