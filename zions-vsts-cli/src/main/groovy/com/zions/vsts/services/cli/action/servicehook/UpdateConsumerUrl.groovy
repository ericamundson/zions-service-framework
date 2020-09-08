package com.zions.vsts.services.cli.action.servicehook;

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
class UpdateConsumerUrl implements CliAction {
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
		//String publisherInputsYaml = data.getOptionValues('publisherInputs')[0]
		eventTypes.each { String eventType -> 
			def subscriptionData = [consumerId: 'webHooks', eventType: eventType, publisherId: 'tfs']
			//subscriptionData.publisherInputs = new JsonBuilder(publisherInputsYaml).getContent()
			
			String projects = data.getOptionValues('projects')[0]
			if ( projects == 'all') {
				def allProjects = projectManagementService.getProjects('')
				allProjects.'value'.each { project ->
					def sub = subscriptionService.getSubscription(project, subscriptionData)
					if (sub && sub.status == "enabled") {
						if  (sub.consumerInputs.url != consumerUrl) {
							sub.consumerInputs.url = consumerUrl
							sub.consumerInputs.basicAuthUsername = consumerUserName
							sub.consumerInputs.basicAuthPassword = consumerPassword
							subscriptionService.updateSubscription(project, sub)
							log.info("Updated URL for ${eventType} subscription in project ${project.name}.")
						}
					}
				}
			} else {
				String[] projectList = projects.split(',')
				projectList.each { String pName ->
					def project = projectManagementService.getProject('', pName)
					if (project) {
						def sub = subscriptionService.getSubscription(project, subscriptionData)
						if (sub && sub.status == "enabled") {
							if  (sub.consumerInputs.url != consumerUrl) {
								sub.consumerInputs.url = consumerUrl
								sub.consumerInputs.basicAuthUsername = consumerUserName
								sub.consumerInputs.basicAuthPassword = consumerPassword
								subscriptionService.updateSubscription(project, sub)
								log.info("Updated URL for ${eventType} subscription in project ${pName}.")
							}
						}
					}
				}
				
			}
		}
		return null
	}

	@Override
	public Object validate( ApplicationArguments args ) throws Exception {
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