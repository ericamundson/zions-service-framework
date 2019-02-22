package com.zions.vsts.services.cli.action.calculations

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.settings.SettingsManagementService
import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.work.calculations.RollupManagementService

@Component
class PerformRollup implements CliAction {
	
	@Autowired
	ProjectManagementService projectManagementService

	@Autowired
	RollupManagementService rollupManagementService
	
	@Autowired
	WorkManagementService workManagementService
	
	@Autowired
	SettingsManagementService settingsManagementService
	
	@Override
	public Object execute(ApplicationArguments data) {
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		//String query = "Select [System.Id] From WorkItems Where [System.WorkItemType] = 'Feature'"
		settingsManagementService.turnOffNotifications(collection)
		def projects = projectManagementService.getProjects(collection)
		projects.value.each { project ->
			String query = "Select [System.Id] From WorkItems Where [System.TeamProject] = '${project.name}' and [System.WorkItemType] = 'Feature'"
			def wis = workManagementService.getWorkItems(collection, project.name, query)
			wis.workItems.each { wi ->
				String id = "${wi.id}"
				rollupManagementService.rollup(id, false, project.name)
			}
		}
		settingsManagementService.turnOnNotifications(collection)
		return null
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
