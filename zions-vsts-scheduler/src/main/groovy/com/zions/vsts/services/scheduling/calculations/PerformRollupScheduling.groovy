package com.zions.vsts.services.scheduling.calculations;

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.settings.SettingsManagementService
import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.work.calculations.RollupManagementService

/**
 * Performs work rollup to Feature work items.
 * 
 * @author z091182
 *
 */
@Component
class PerformRollupScheduling {
	
	@Autowired
	ProjectManagementService projectManagementService

	@Autowired
	RollupManagementService rollupManagementService
	
	@Autowired
	WorkManagementService workManagementService
	
	@Autowired
	SettingsManagementService settingsManagementService
	
	@Value('${tfs.collection:}')
	String collection;
	
	@Scheduled(cron = '0 23 * * *')
	public Object run() {
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


}
