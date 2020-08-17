package com.zions.pipeline.services.yaml.template.execution

import groovy.json.JsonBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.policy.PolicyManagementService

/**
 * Accepts yaml in the form (publisherInputs will vary by eventType):
 * executables:
 * - type: branchPolicy
 *   projects: ReleaseEngineering
 *   eventTypes: git.push
 *   consumerUrl: https://releaseengineeringprovisioner-zionsbancorporation.msappproxy.net
 *   consumerUserName: svc-cloud-vsbuildagent
 *   consumerPassword: !value "webhookPassword"
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
class BranchPolicy implements IExecutableYamlHandler {
	

	@Autowired
	ProjectManagementService projectManagementService
	
	@Autowired
	PolicyManagementService policyManagementService

	def handleYaml(def yaml, File containedRepo, def locations, String branch) {
		//System.out.println("In handleYaml - yaml:\n" + yaml)
		String pName = "${yaml.project}"
		//System.out.println("BranchPolicy::handleYaml - Calling projectManagementService.getProject for: " + pName)
		def project = projectManagementService.getProject('', pName)
		if (project) {
		
			String[] branchNames = yaml.branchNames.split(',')
			branchNames.each { String branchName ->
				def repoData = [consumerId: 'webHooks', consumerActionId: 'httpRequest', eventType: eventType, publisherId: 'tfs', consumerInputs: [url: yaml.consumerUrl, basicAuthUsername: yaml.consumerUserName, basicAuthPassword: yaml.consumerPassword], publisherInputs:[], resourceVersion: '1.0', scope: 1]
				//def subscriptionData.publisherInputs = new JsonBuilder(yaml.publisherInputs).getContent()
				//System.out.println("BranchPolicy::handleYaml - subscriptionData:\n" + subscriptionData)
				
				policyManagementService.ensurePolicies('', repoData, branchName)
			}
		}
	}
}
