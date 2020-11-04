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
class ExportTeamRepos implements CliAction {
	PermissionsManagementService permissionsManagementService
	ProjectManagementService projectManagementService
	CodeManagementService codeManagementService
	MemberManagementService memberManagementService
	
	@Autowired
	public ExportTeamRepos(PermissionsManagementService permissionsManagementService,
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
		String teamName = data.getOptionValues('team.name')[0]
		String fileName = data.getOptionValues('out.file.name')[0]
		def projectData = projectManagementService.getProject(collection, project)
		def repos = codeManagementService.getRepos(collection, projectData) //, teamName)
		File f = new File(fileName)
		def o = f.newDataOutputStream()
		repos.value.each { repo ->
			def pullRequests = codeManagementService.getPullRequests(collection, projectData, repo)
			o << "${repo.name},${repo.webUrl},\"" + getContributors(pullRequests) + "\"\n"
		}
		
		o.close()
		return null
	}
	String getContributors(def pullRequests) {
		if (pullRequests && pullRequests.count > 0) {
			def contributors = []
			pullRequests.value.each { pr ->
				String name = "${pr.createdBy.displayName}"
				if (!contributors.contains(name))
					contributors.add(name)
			}
			return contributors.join(",")
		}
		else
			return ''
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.project', 'team.name', 'out.file.name']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	
}