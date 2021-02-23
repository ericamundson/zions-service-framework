package com.zions.pipeline.services.yaml.template.execution

import groovy.json.JsonBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.zions.pipeline.services.mixins.FeedbackTrait
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.policy.PolicyManagementService
import com.zions.vsts.services.code.CodeManagementService

/**
 * Accepts yaml in the form:
 * <pre>
 * executables:
 * - type: branchPolicy
 *   context: zionseto
 *   projects: ReleaseEngineering
 *   repoName: arepo
 *   branchNames: master
 *   policyData:
 *     buildData:
 *       ciBuildName: arepo-ci
 *       ciBuildFile:  .pipeline/ado/build-ci.yml
 *     approvalData:
 *       minApprovers: 1
 * </pre>
 * @author z091556
 *
 */
@Component
class BranchPolicy implements IExecutableYamlHandler, FeedbackTrait {
	

	@Autowired
	ProjectManagementService projectManagementService
	
	@Autowired
	PolicyManagementService policyManagementService
	
	@Autowired
	CodeManagementService codeManagementService

	def handleYaml(def yaml, File containedRepo, def locations, String branch, String pName, String pipelineId = null) {
		//System.out.println("In handleYaml - yaml:\n" + yaml)
		if (yaml.project) {
			pName = yaml.project
		}
		String ciBuildName = null
		if (yaml.ciBuildName) {
			ciBuildName = "${yaml.ciBuildName}"
		}
		String releaseBuildFileName = null
		if (yaml.releaseBuildFileName) {
			releaseBuildFileName = yaml.releaseBuildFileName
		}
		//System.out.println("BranchPolicy::handleYaml - Calling projectManagementService.getProject for: " + pName)
		def project = projectManagementService.getProject('', pName)
		def repoData = codeManagementService.getRepo('', project, yaml.repository)
		def policyData = yaml.policyData
		
		if (project) {
		
			String branchName = yaml.branchName
			policyManagementService.clearBranchPolicies('', project, repoData.id, branchName)
			//def subscriptionData.publisherInputs = new JsonBuilder(yaml.publisherInputs).getContent()
			//System.out.println("BranchPolicy::handleYaml - subscriptionData:\n" + subscriptionData)
			
			policyManagementService.ensurePolicies('', repoData, branchName, policyData)
			logInfo(pipelineId, 'Updated branch policy')
		} else {
			logError(pipelineId, "ADO project was not found:  ${pName}")
		}
	}
}
