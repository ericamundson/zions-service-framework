package com.zions.vsts.services.cli.action.release;

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.build.BuildManagementService
import com.zions.vsts.services.release.ReleaseManagementService
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper


@Component
class SyncProjectReleases implements CliAction {
	ReleaseManagementService releaseManagementService
	
	@Autowired
	public SyncProjectReleases(ReleaseManagementService releaseService) {
		this.releaseManagementService = releaseService
	}

	@Override
	public def execute(ApplicationArguments data) {
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		String project = data.getOptionValues('tfs.project')[0]
		String templateName = data.getOptionValues('template.name')[0]
		String xldEndpoint = data.getOptionValues('xld.endpoint')[0]
		String folder = data.getOptionValues('tfs.release.folder')[0]
		String team = data.getOptionValues('tfs.team')[0]
		def template = getResource(templateName)
		releaseManagementService.ensureReleaseFolder(collection, project, folder)
		return releaseManagementService.ensureReleases(collection, project, template, xldEndpoint, folder, team)
	}
	
	public def getResource(String name) {
		def template = null
		try {
		def s = getClass().getResourceAsStream("/release_templates/${name}.json")
		JsonSlurper js = new JsonSlurper()
		template = js.parse(s)
		} catch (e) {}
		return template
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.token',  'tfs.project', 'template.name', 'xld.endpoint', 'tfs.release.folder', 'tfs.team']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	
}