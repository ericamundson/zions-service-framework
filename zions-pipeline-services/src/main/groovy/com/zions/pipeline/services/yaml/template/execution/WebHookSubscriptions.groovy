package com.zions.pipeline.services.yaml.template.execution
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component


import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.servicehooks.SubscriptionService

@Component
class WebHookSubscriptions implements IExecutableYamlHandler {
	

	@Autowired
	ProjectManagementService projectManagementService
	
	@Autowired
	SubscriptionService subscriptionService

	def handleYaml(def yaml, File containedRepo, def locations) {
		String[] eventTypes = yaml.eventTypes.split(',')
		eventTypes.each { String eventType -> 
			def subscriptionData = [consumerId: 'webHooks', consumerActionId: 'httpRequest', eventType: eventType, publisherId: 'tfs', consumerInputs: [url: yaml.consumerUrl, basicAuthUsername: yaml.consumerUserName, basicAuthPassword: yaml.consumerPassword], publisherInputs:[areaPath:'', workItemType:'', projectId: null], resourceVersion: '1.0', scope: 1]
			
			String projects = "${yaml.projects}"
			if ( projects == 'all') {
				def allProjects = projectManagementService.getProjects('')
				allProjects.'value'.each { project ->
					subscriptionService.ensureSubscription(project, subscriptionData)
				}
			} else {
				String[] projectList = projects.split(',')
				projectList.each { String pName ->
					def project = projectManagementService.getProject('', pName)
					if (project) {
						subscriptionService.ensureSubscription(project, subscriptionData)
					}
				}
				
			}
		}
	}
}
