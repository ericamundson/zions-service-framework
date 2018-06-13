package com.zions.vsts.services.cli.action.build;

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.build.BuildManagementService
import com.zions.vsts.services.code.CodeManagementService
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper


@Component
class ReviseRelease implements CliAction {
	BuildManagementService buildManagementService
	ProjectManagementService projectManagementService
	
	@Autowired
	public ReviseRelease(BuildManagementService buildService,
		ProjectManagementService projectManagementService) {
		this.buildManagementService = buildService
		this.projectManagementService = projectManagementService
	}

	@Override
	public def execute(ApplicationArguments data) {
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		String project = data.getOptionValues('tfs.project')[0]
		String repoList = data.getOptionValues('repo.list')[0]
		String releaseLabel = data.getOptionValues('release.label')[0]
		def projectData = projectManagementService.getProject(collection, project)
		def build = buildManagementService.reviseReleaseLabels(collection, projectData, repoList, releaseLabel)
		return null
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.token',  'tfs.project', 'release.label', 'repo.list']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	
}