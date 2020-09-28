package com.zions.pipeline.services.yaml.template.execution

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.common.services.vault.VaultService
import com.zions.pipeline.services.git.GitService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.policy.PolicyManagementService
import com.zions.pipeline.services.mixins.PropertiesSanitizerTrait

/**
 * This handler support yaml in the following form:
 * type: sanitizeProperties
 * filePath: .pipeline/xebialabs/secrets.xlvals
 * vault:
 *   engine: secret
 *   path: WebCMS
 *   
 *   This yaml handler will convert all property elements with keys into Vault for secret value
 *   
 * @author z091182
 *
 */
@Component
class SanitizeProperties implements IExecutableYamlHandler, PropertiesSanitizerTrait {

	def handleYaml(def yaml, File repo, def locations, String branch, String project) {
		String propertyPath = "${repo.absolutePath}/${yaml.filePath}"
		File pFile = new File(propertyPath)
		String engine = 'secret'
		String path = project
		if (yaml.vault) {
			engine = yaml.vault.engine
			path = yaml.vault.path
		}
		if (pFile.exists()) {
			sanitize(repo, branch, project, pFile, engine, path)
		}
	}
	

}
