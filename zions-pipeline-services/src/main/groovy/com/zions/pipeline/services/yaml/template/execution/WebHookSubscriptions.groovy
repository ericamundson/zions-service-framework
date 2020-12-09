package com.zions.pipeline.services.yaml.template.execution
import groovy.json.JsonBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.zions.common.services.vault.VaultService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.servicehooks.SubscriptionService
import com.zions.pipeline.services.mixins.ReadSecretsTrait
/**
 * Accepts yaml in the form (publisherInputs will vary by eventType):
 * executables:
 * - type: webHookSubscriptions
 *   context: eto-dev
 *   projects: ReleaseEngineering
 *   eventTypes: git.push
 *   consumerUrl: https://releaseengineeringprovisioner-zionsbancorporation.msappproxy.net
 *   #optional Vault key
 *   consumerUserName: ${webhook.user}
 *   #optional Vault key
 *   consumerPassword: ${webook.password}
 *   publisherInputs:
 *     repository: ""
 *     branch: ""
 *     pushedBy: ""
 *     projectId: ""
 *
 * @author z091556
 *
 */
@Component
class WebHookSubscriptions implements IExecutableYamlHandler, ReadSecretsTrait {
	@Autowired
	VaultService vaultService


	@Autowired
	ProjectManagementService projectManagementService
	
	@Autowired
	SubscriptionService subscriptionService

	@Autowired
	@Value('${webhook.user:}')
	String consumerUserName

	@Autowired
	@Value('${webhook.password:}')
	String consumerPassword

	def handleYaml(def yaml, File containedRepo, def locations, String branch, String projectName) {
		//System.out.println("In handleYaml - yaml:\n" + yaml)
		// TODO: Collection of event types dosn't makes sense as each different event type will have a different set of publisherInputs
		String[] eventTypes = yaml.eventTypes.split(',')
		def vaultSecrets = null
		if (yaml.vault) {
			vaultSecrets = vaultService.getSecrets(yaml.vault.engine, yaml.vault.paths)
			if (yaml.consumerUserName && yaml.consumerPassword) {
				consumerUserName = getSecret(vaultSecrets, yaml.consumerUserName)
				consumerPassword = getSecret(vaultSecrets, yaml.consumerPassword)
			} else {
				consumerUserName = vaultSecrets['webhook.user']
				consumerPassword = vaultSecrets['webhook.password']
			}
		}
		eventTypes.each { String eventType ->
			//def subscriptionData = [consumerId: 'webHooks', consumerActionId: 'httpRequest', eventType: eventType, publisherId: 'tfs', consumerInputs: [url: yaml.consumerUrl, basicAuthUsername: yaml.consumerUserName, basicAuthPassword: yaml.consumerPassword], publisherInputs:[], resourceVersion: '1.0', scope: 1]
			def subscriptionData = [consumerId: 'webHooks', consumerActionId: 'httpRequest', eventType: eventType, publisherId: 'tfs', consumerInputs: [url: yaml.consumerUrl, basicAuthUsername: consumerUserName, basicAuthPassword: consumerPassword], publisherInputs:[], resourceVersion: '1.0', scope: 1]
			subscriptionData.publisherInputs = new JsonBuilder(yaml.publisherInputs).getContent()
			//System.out.println("In handleYaml - subscriptionData:\n" + subscriptionData)
			
			String projects = "${yaml.projects}"
			if ( projects == 'all') {
				def allProjects = projectManagementService.getProjects('')
				allProjects.'value'.each { project ->
					subscriptionService.ensureSubscription(project, subscriptionData)
				}
			} else {
				String[] projectList = projects.split(',')
				projectList.each { String pName ->
					//System.out.println("In handleYaml - Calling projectManagementService.getProject for: " + pName)
					def project = projectManagementService.getProject('', pName)
					if (project) {
						subscriptionService.ensureSubscription(project, subscriptionData)
					}
				}
				
			}
		}
	}
}
