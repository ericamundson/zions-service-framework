package com.zions.vsts.services.cli.action.build;

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.build.BuildManagementService
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper


@Component
class FixTaggingOutputVarAll implements CliAction {
	BuildManagementService buildManagementService
	ProjectManagementService projectManagementService
	
	@Autowired
	public FixTaggingOutputVarAll(BuildManagementService buildService, ProjectManagementService projectManagementService) {
		this.buildManagementService = buildService
		this.projectManagementService = projectManagementService
	}

	@Override
	public def execute(ApplicationArguments data) {
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		String newOutputVarName = ""
		if (data.getOptionValues('newOutputVar') != null) {
			newOutputVarName = data.getOptionValues('newOutputVar')[0]
		}
		// default new newOutputVarName to 'zions.buildnumber' if not provided
		if (newOutputVarName == "") {
			newOutputVarName = "zions.buildnumber"
		}
		System.out.println("Getting projects ...")
		def projects = projectManagementService.getProjects(collection)
		projects.value.each { projectData ->
			// process builds for project
			updateOutputVar(collection, projectData, newOutputVarName)
		}
		return null
	}

	private Object updateOutputVar( def collection, def projectData, def newOutputVarName ) {
		def result = buildManagementService.updateTaggingTasks(collection, projectData, newOutputVarName)
		return result
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.token']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	
}