package com.zions.vsts.services.cli.action.project

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.admin.project.ProjectManagementService

/**
 * List out project and team data.
 * 
 * @author Matt
 *
 */
@Component
class ListProjectsAndTeams implements CliAction {
	
	@Autowired
	ProjectManagementService projectManagementService

	@Override
	public Object execute(ApplicationArguments args) {
		def projects = projectManagementService.getProjects('')
		projects.'value'.each { project -> 
			// TODO: Do some code to write project stuff
			String pName = "${project.name}"
			def teams = projectManagementService.getTeams('', pName)
			// TODO: Loop to write some team stuff.
		}
		return null
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.token', 'export.dir']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}

}