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
		def repoData = bBCodeManagmentService.getProjectRepoData(project)
//		memberData.members.each { member ->
//			def teams = memberManagmentService.addMember(collection, member.id, member.teams)
//			
//		}
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