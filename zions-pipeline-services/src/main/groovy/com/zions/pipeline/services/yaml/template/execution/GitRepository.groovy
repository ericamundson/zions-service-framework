package com.zions.pipeline.services.yaml.template.execution
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component


import com.zions.vsts.services.build.BuildManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService

@Component
class GitRepository implements IExecutableYamlHandler {
	@Autowired
	CodeManagementService codeManagementService
	
	@Autowired
	ProjectManagementService projectManagementService
	
	public GitRepository() {
		
	}
	
	def handleYaml(def yaml, File repo, def locations) {
		String projectName = yaml.project
		String repoName = yaml.name
		def project = projectManagementService.getProject('', projectName)
		def repository = codeManagementService.ensureRepo('', project, repoName)
		
		yaml.branches.each { def branch ->
			String baseName = branch.baseName
			String branchName = branch.name
			def tbranch = codeManagementService.ensureBranch('', projectName, repoName, baseName, branchName)
		}
	}
}
