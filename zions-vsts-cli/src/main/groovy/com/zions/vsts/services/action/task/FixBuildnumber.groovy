package com.zions.vsts.services.action.task;

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.distributedtask.TaskManagementService
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper


@Component
class FixBuildnumber implements CliAction {
	TaskManagementService taskManagementService
	ProjectManagementService projectManagementService
	
	@Autowired
	public FixBuildnumber(TaskManagementService taskService, ProjectManagementService projectManagementService) {
		this.taskManagementService = taskService
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
		// check for single task group
		String taskGroupId = ""
		if (data.getOptionValues('group.id') != null) {
			taskGroupId = data.getOptionValues('group.id')[0]
		}
		String newOutputVarName = ""
		if (data.getOptionValues('newOutputVar') != null) {
			newOutputVarName = data.getOptionValues('newOutputVar')[0]
		}
		// default new newOutputVarName to 'zions.buildnumber' if not provided
		if (newOutputVarName == "") {
			newOutputVarName = "zions.buildnumber"
		}
		if (taskGroupId != null && taskGroupId != "") {
			System.out.println("Updating task group "+taskGroupId)
			def result = taskManagementService.updateBuildnumberRef(collection, projectData, taskGroupId, newOutputVarName)
		} else {
			def result = taskManagementService.updateBuildnumberRefs(collection, projectData, newOutputVarName)
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