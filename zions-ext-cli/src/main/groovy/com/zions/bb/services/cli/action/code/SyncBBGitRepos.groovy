package com.zions.bb.services.cli.action.code;

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.bb.services.code.BBCodeManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.code.CodeManagementService
import groovy.json.JsonSlurper


@Component
class SyncBBGitRepos implements CliAction {
	CodeManagementService codeManagmentService
	BBCodeManagementService bBCodeManagmentService
	
	@Autowired
	public SyncBBGitRepos(CodeManagementService codeManagmentService, BBCodeManagementService bBCodeManagmentService) {
		this.codeManagmentService = codeManagmentService
		this.bBCodeManagmentService = bBCodeManagmentService;
	}

	@Override
	public def execute(ApplicationArguments data) {
		String collection = data.getOptionValues('tfs.collection')[0]
		String project = data.getOptionValues('bb.project')[0]
		String outproject = data.getOptionValues('tfs.project')[0]
		String bbUser = data.getOptionValues('bb.user')[0]
		String bbPassword = data.getOptionValues('bb.password')[0]
		def repos = bBCodeManagmentService.getProjectRepoUrls(project)
		repos.each { repo ->
			def url = repo.url.replace("${bbUser}@", '')
			codeManagmentService.importRepo(collection, outproject, repo.name, url, bbUser, bbPassword)			
		}
		return null;
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.collection', 'tfs.token',  'bb.url', 'bb.user', 'bb.password', 'bb.project', 'tfs.project']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	
}