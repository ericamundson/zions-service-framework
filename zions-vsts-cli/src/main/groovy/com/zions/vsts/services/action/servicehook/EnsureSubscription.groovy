package com.zions.vsts.services.action.servicehook;

import groovy.json.JsonBuilder
import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.servicehooks.SubscriptionService
import groovy.util.logging.Slf4j


@Component
@Slf4j
class EnsureSubscription implements CliAction {
	@Autowired
	SubscriptionService subscriptionService

	@Autowired
	ProjectManagementService projectManagementService
	
	@Override
	public def execute(ApplicationArguments data) {
		String[] eventTypes = data.getOptionValues('eventTypes')[0].split(',')
		String consumerUrl = data.getOptionValues('consumerUrl')[0]
		String consumerUserName = data.getOptionValues('consumerUserName')[0]
		String consumerPassword = data.getOptionValues('consumerPassword')[0]
		// TODO: This initially worked passing as '{areaPath: "", workItemType: "Bug", projectId: null}' and converting the string to JSON (line 37).
		//  No longer works as it treats the JSON as a string downstream and causes an exception when trying to use dot reference on items in publisherInputs
		String publisherInputsYaml = data.getOptionValues('publisherInputs')[0]
		//def publisherInputsYaml = [areaPath: "", workItemType: "Bug", projectId: null]
		//System.out.println("In execute - publisherInputsYaml :\n${publisherInputsYaml}" )
		eventTypes.each { String eventType -> 
			def subscriptionData = [consumerId: 'webHooks', consumerActionId: 'httpRequest', eventType: eventType, publisherId: 'tfs', consumerInputs: [url: consumerUrl, basicAuthUsername: consumerUserName, basicAuthPassword: consumerPassword], publisherInputs:[], resourceVersion: '1.0', scope: 1]
			subscriptionData.publisherInputs = new JsonBuilder(publisherInputsYaml).getContent()
			//System.out.println("subscriptionData :\n${subscriptionData}" )
			String projects = data.getOptionValues('projects')[0]
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
		return null
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.token', 'eventTypes', 'projects']
		required.each { name ->
			if (!args.containsOption(name)) {
				log.debug("Missing required argument:  ${name}.  Exiting ...")
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	
}