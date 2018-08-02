package com.zions.bb.services.cli.action.code;

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.bb.services.code.BBCodeManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.permissions.PermissionsManagementService
import groovy.json.JsonSlurper


@Component
class SyncBBGitRepos implements CliAction {
	CodeManagementService codeManagmentService
	BBCodeManagementService bBCodeManagmentService
	PermissionsManagementService permissionsManagementService
	
	@Autowired
	public SyncBBGitRepos(CodeManagementService codeManagmentService, 
		BBCodeManagementService bBCodeManagmentService,
		PermissionsManagementService permissionsManagementService) {
		this.codeManagmentService = codeManagmentService
		this.bBCodeManagmentService = bBCodeManagmentService;
		this.permissionsManagementService = permissionsManagementService
	}

	@Override
	public def execute(ApplicationArguments data) {
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		String project = data.getOptionValues('bb.project')[0]
		String outproject = data.getOptionValues('tfs.project')[0]
		String bbUser = data.getOptionValues('bb.user')[0]
		String bbPassword = data.getOptionValues('bb.password')[0]
		String teamName = data.getOptionValues('tfs.team')[0]
		String templateName = data.getOptionValues('grant.template')[0]
		def repos = bBCodeManagmentService.getProjectRepoUrls(project)
		repos.each { repo ->
			def url = repo.url.replace("${bbUser}@", '')
			codeManagmentService.importRepoCLI(collection, outproject, repo.name, url, bbUser, bbPassword)	
			permissionsManagementService.ensureTeamToRepo(collection, outproject, repo.name, teamName, templateName)		
		}
		File git = new File('git')
		if (git.exists()) {
			git.deleteDir()
		}
		return null;
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.token',  'bb.url', 'bb.user', 'bb.password', 'bb.project', 'tfs.project', 'tfs.team', 'grant.template']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	
}