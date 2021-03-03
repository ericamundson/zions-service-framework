package com.zions.pipeline.services.yaml.template.execution
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component


import com.zions.vsts.services.build.BuildManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.policy.PolicyManagementService
import com.zions.pipeline.services.mixins.FeedbackTrait
import com.zions.vsts.services.admin.project.ProjectManagementService

import groovy.util.logging.Slf4j
import groovy.yaml.YamlBuilder

/**
 * Accepts yaml in the form:
 * <pre>
 * executables:
 * - name: reponame 
 *   type: gitRepository
 *   project: projectname
 *   branches:
 *   - name: feature/support
 *     baseName: master
 *  </pre>   
 * @author z091182
 *
 */
@Component
@Slf4j
class GitRepository implements IExecutableYamlHandler, FeedbackTrait {
	@Autowired
	CodeManagementService codeManagementService
	
	@Autowired
	ProjectManagementService projectManagementService
	
	@Autowired
	PolicyManagementService policyManagementService

	public GitRepository() {
		
	}
	
	def handleYaml(def yaml, File repo, def locations, String inBranch, String projectName, String pipelineId = null, String userName = null) {
//		String projectName = yaml.project
		String repoName = yaml.name
//		def answers = yaml.answers
//		String answersStr = "${repoName}"
//		if (answers) {
//			YamlBuilder yb = new YamlBuilder()
//			yb(answers)
//			answersStr = yb.toString()
//		}
		if (yaml.project) {
			projectName = yaml.project
		}
		logInfo(pipelineId, "Ensure repo: ${repoName}")
		def project = projectManagementService.getProject('', projectName)
		def repository = codeManagementService.ensureRepo('', project, repoName)
		try {
			logInfo(pipelineId, "Ensuring README.md!")
			def policies = policyManagementService.clearBranchPolicies('', project, repository.id, 'refs/heads/master')
			codeManagementService.ensureFile('', project, repository, '/README.md', "##${repoName}")
			policyManagementService.restoreBranchPolicies('', project, repository.id, 'refs/heads/master', policies)
		} catch (e) {
			log.error("Failed file push::  ${e.message}")
			logError(pipelineId, "${e.message}")
		}
		
		yaml.branches.each { def branch ->
			logInfo(pipelineId, "Ensure branch: ${branch.name}")
			String baseName = branch.baseName
			String branchName = branch.name
			def tbranch = codeManagementService.ensureBranch('', projectName, repoName, baseName, branchName)
		}
	}
}
