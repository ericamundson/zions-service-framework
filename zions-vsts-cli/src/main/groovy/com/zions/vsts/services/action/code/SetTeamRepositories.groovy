package com.zions.vsts.services.action.code;

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.permissions.PermissionsManagementService
import groovy.json.JsonSlurper


@Component
class SetTeamRepositories implements CliAction {
	PermissionsManagementService permissionsManagementService
	
	@Autowired
	public SetTeamRepositories(PermissionsManagementService permissionsManagementService) {
		this.permissionsManagementService = permissionsManagementService
	}

	@Override
	public def execute(ApplicationArguments data) {
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		String inFile = data.getOptionValues('teamrepos.file.name')[0]
		String template = data.getOptionValues('permissiontemplate.resource')[0]
		File mFile = new File(inFile)
		JsonSlurper s = new JsonSlurper()
		def teamRepoData = s.parseText(mFile.text)
		teamRepoData.projects.each { project ->
			project.teams.each { team ->
				team.repos.each { repo ->
					def teams = permissionsManagementService.ensureTeamToRepo(collection, project.name, repo.name, team.name, template)
					
				}
			}
			
		}
		return null;
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.token', 'teamrepos.file.name', 'permissiontemplate.resource']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	
}