package com.zions.vsts.services.action.code;

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.build.BuildManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.permissions.PermissionsManagementService
import com.zions.vsts.services.release.ReleaseManagementService
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper


@Component
class ExportTeamRepoPermissions implements CliAction {
	PermissionsManagementService permissionsManagementService
	ProjectManagementService projectManagementService
	CodeManagementService codeManagementService
	MemberManagementService memberManagementService
	
	@Autowired
	public ExportTeamRepoPermissions(PermissionsManagementService permissionsManagementService,
		ProjectManagementService projectManagementService,
		CodeManagementService codeManagementService,
		MemberManagementService memberManagementService) {
		this.permissionsManagementService = permissionsManagementService
		this.projectManagementService = projectManagementService
		this.codeManagementService = codeManagementService
		this.memberManagementService = memberManagementService
	}

	@Override
	public def execute(ApplicationArguments data) {
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		String project = data.getOptionValues('tfs.project')[0]
		String repo = data.getOptionValues('tfs.repo')[0]
		String teamName = data.getOptionValues('team.name')[0]
		String fileName = data.getOptionValues('out.file.name')[0]
		def projectData = projectManagementService.getProject(collection, project)
		def repoData = codeManagementService.getRepo(collection, projectData, repo)
		def team = memberManagementService.getTeam(collection, projectData, teamName)
		def build = permissionsManagementService.getRepoPermissions(collection, projectData, repoData, team)
		File f = new File(fileName)
		def o = f.newDataOutputStream()
		o << new JsonBuilder(build).toPrettyString()
		o.close()
		return null
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.token',  'tfs.project', 'tfs.repo', 'team.name', 'out.file.name']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	
}