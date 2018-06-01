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
class ExportBuild implements CliAction {
	BuildManagementService buildManagementService
	ProjectManagementService projectManagementService
	CodeManagementService codeManagementService
	
	@Autowired
	public ExportBuild(BuildManagementService buildService,
		ProjectManagementService projectManagementService,
		CodeManagementService codeManagementService) {
		this.buildManagementService = buildService
		this.projectManagementService = projectManagementService
		this.codeManagementService = codeManagementService
	}

	@Override
	public def execute(ApplicationArguments data) {
		String collection = data.getOptionValues('tfs.collection')[0]
		String project = data.getOptionValues('tfs.project')[0]
		String buildName = data.getOptionValues('build.name')[0]
		String fileName = data.getOptionValues('out.file.name')[0]
		def projectData = projectManagementService.getProject(collection, project)
		def build = buildManagementService.getBuild(collection, projectData, buildName)
		File f = new File(fileName)
		def o = f.newDataOutputStream()
		o << new JsonBuilder(build).toPrettyString()
		o.close()
		return null
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.collection', 'tfs.token',  'tfs.project', 'build.name', 'out.file.name']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	
}