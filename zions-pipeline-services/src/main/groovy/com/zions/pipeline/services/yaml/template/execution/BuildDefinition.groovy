package com.zions.pipeline.services.yaml.template.execution
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component


import com.zions.vsts.services.build.BuildManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService

@Component
class BuildDefinition implements IExecutableYamlHandler {
	
	@Autowired
	BuildManagementService buildManagementService
	@Autowired
	CodeManagementService codeManagementService
	@Autowired
	ProjectManagementService projectManagementService

	def handleYaml(def yaml) {
		
		def project = projectManagementService.getProject('', yaml.project)
		def build = buildManagementService.getBuild('', project, yaml.name)
		def queue = buildManagementService.getQueue('',project, yaml.queue)
		if (!build) {
			def repo = codeManagementService.getRepo('', project, yaml.repository)
			def bDef = [name: yaml.name, project: yaml.project, repository: [id: repo.id, url: repo.url, type: 'TfsGit'], process: [yamlFilename: yaml.buildyaml, type:2], queue: queue ]
			def query = ['api-version':'5.1']
			buildManagementService.writeBuildDefinition('', project, bDef, query)
		} else {
			def repo = codeManagementService.getRepo('', project, yaml.repository)
			def bDef = [id: build.id, name: yaml.name, project: yaml.project, repository: [id: repo.id, url: repo.url, type: 'TfsGit'], process: [yamlFilename: yaml.buildyaml, type:2], queue: queue ]
//			def bDef = [id: build.id, name: yaml.name, project: yaml.project, repository: [id: repo.id, url: repo.url, type: 'TfsGit'], process: [yamlFilename: yaml.buildyaml, type:2] ]
			def query = ['api-version':'5.1']
			buildManagementService.updateBuildDefinition('', project, bDef, query)
			
		}
	}
}
