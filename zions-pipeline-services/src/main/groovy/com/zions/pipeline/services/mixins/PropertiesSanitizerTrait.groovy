package com.zions.pipeline.services.mixins

import org.springframework.beans.factory.annotation.Autowired

import com.zions.common.services.vault.VaultService
import com.zions.pipeline.services.git.GitService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.policy.PolicyManagementService

trait PropertiesSanitizerTrait {
	@Autowired
	GitService gitService

	@Autowired
	CodeManagementService codeManagementService

	@Autowired
	ProjectManagementService projectManagementService

	@Autowired
	PolicyManagementService policyManagementService

	@Autowired
	VaultService vaultService

	def sanitize(File repo, String branch, String project, File propertyFile, String engine, String path) {
		gitService.reset(repo)
		Map vaultSecrets = [:]
		buildSecrets(propertyFile, vaultSecrets)
		if (vaultSecrets.size() > 0) {
			def result = vaultService.ensureSecrets(engine, path, vaultSecrets)
			if (result) {
				String repoName = repo.name
				def projectData = projectManagementService.getProject('', project)
				def repoData = codeManagementService.getRepo('', projectData, repoName)
				def policies = policyManagementService.clearBranchPolicies('', projectData, repoData.id, branch)
				gitService.pushChanges(repo)
				policyManagementService.restoreBranchPolicies('', projectData, repoData.id, branch, policies)
			}
		}
	}
	
	def buildSecrets(File secretsFile, vaultSecrets) {
		if (secretsFile.exists()) {
			Properties props = new Properties()
			def is = secretsFile.newDataInputStream()
			props.load(is)
			is.close()
			for (String name in props.stringPropertyNames()) {
				def val = props.getProperty(name)
				String value = "${val}"
				if (!value.startsWith('${')) {
					vaultSecrets[name] = value
					if (value) {
						props.setProperty(name, "\${${name}}")
					}
				}
			}
			def w = secretsFile.newWriter()
			props.store(w, 'updated secrets')
			w.close()
		}
		return vaultSecrets
	}
}
