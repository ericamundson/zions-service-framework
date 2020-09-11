package com.zions.vsts.services.action.build;

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
class UdeploytoXLDBuilds implements CliAction {
	BuildManagementService buildManagementService
	ProjectManagementService projectManagementService
	
	@Autowired
	public UdeploytoXLDBuilds(BuildManagementService buildService, ProjectManagementService projectManagementService) {
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
		//String repoList = data.getOptionValues('repo.list')[0]
		System.out.println("Getting project ...")
		def projectData = projectManagementService.getProject(collection, project)
		if (projectData == null) {
			System.out.println("Exception occurred trying to get project data.  Returning null ...")
			return null
		}
		// check for single build def
		String buildId = ""
		if (data.getOptionValues('build.id') != null) {
			buildId = data.getOptionValues('build.id')[0]
		}
		String dut = ""
		if (data.getOptionValues('deleteUnwanted') != null) {
			dut = data.getOptionValues('deleteUnwanted')[0]
		}
		boolean deleteUnwantedTasks = false
		if (dut != null && (dut == "true" || dut == "yes")) {
			deleteUnwantedTasks = true
		}
		if (buildId != null && buildId != "") {
			System.out.println("Updating build "+buildId)
			def result = buildManagementService.updateBuild(collection, projectData, buildId, deleteUnwantedTasks)
		} else {
			def result = buildManagementService.updateBuilds(collection, projectData, deleteUnwantedTasks)
		}
		return null
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.token',  'tfs.project']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	
}