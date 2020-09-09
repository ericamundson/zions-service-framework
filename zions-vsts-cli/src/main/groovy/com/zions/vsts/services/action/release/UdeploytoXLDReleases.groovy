package com.zions.vsts.services.action.release;

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.release.ReleaseManagementService
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper


@Component
class UdeploytoXLDReleases implements CliAction {
	ReleaseManagementService releaseManagementService
	ProjectManagementService projectManagementService
	
	@Autowired
	public UdeploytoXLDReleases(ReleaseManagementService releaseService, ProjectManagementService projectManagementService) {
		this.releaseManagementService = releaseService
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
		// check for single release def
		String releaseId = ""
		if (data.getOptionValues('release.id') != null) {
			releaseId = data.getOptionValues('release.id')[0]
		}
		String dut = ""
		if (data.getOptionValues('deleteUnwanted') != null) {
			dut = data.getOptionValues('deleteUnwanted')[0]
		}
		boolean deleteUnwantedTasks = false
		if (dut != null && (dut == "true" || dut == "yes")) {
			deleteUnwantedTasks = true
		}
		if (releaseId != null && releaseId != "") {
			System.out.println("Updating release "+releaseId)
			def result = releaseManagementService.updateRelease(collection, projectData, releaseId, deleteUnwantedTasks)
		} else {
			def result = releaseManagementService.updateReleases(collection, projectData, deleteUnwantedTasks)
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