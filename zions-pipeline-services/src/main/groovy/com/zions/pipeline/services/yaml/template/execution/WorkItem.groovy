package com.zions.pipeline.services.yaml.template.execution
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.zions.pipeline.services.mixins.FeedbackTrait
import com.zions.vsts.services.work.WorkManagementService

@Component
class WorkItem implements IExecutableYamlHandler, FeedbackTrait {
	
	@Autowired
	WorkManagementService workManagementService
	
	public WorkItem() {
		
	}
	
	def handleYaml(def yaml, File repo, def locations, String branch, String project, String pipelineId = null, String userName = null) {
		if (yaml.project) {
			project = yaml.project
		}
		//String project = yaml.project
		String title = yaml.title
		String description = yaml.description
		String wiType = yaml.wiType
		def data = [[op: 'add', path: '/fields/System.Title', value: title], [op: 'add', path: '/fields/System.Description', value: description]]
		if (yaml.areaPath) {
			def op = [op: 'add', path: '/fields/System.AreaPath', value: yaml.areaPath]
			data.add(op)
		}
		String query = "Select [System.Id], [System.Title] From WorkItems Where [System.TeamProject] = '${project}' AND [System.WorkItemType] = '${wiType}' and [System.Title] = '${title}'"
		def wis = workManagementService.getWorkItems('', project, query)
		if (wis.workItems.size() > 0) {
			def wi = wis.workItems[0]
			workManagementService.updateWorkItem('', project, wi.id, data)
		} else {
			workManagementService.createWorkItem('', project, wiType, data)
		}
		
	}
		
}
