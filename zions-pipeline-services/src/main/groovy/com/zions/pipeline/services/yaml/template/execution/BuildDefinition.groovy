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
			def trigger = [batchChanges: false, pollingJobId: null, pollingInterval: 0, pathFilters:[], branchFilters: ['+refs/heads/master'], defaultSettingsSourceType: 2, isSettingsSourceOptionSupported: true, settingsSourceType: 2, triggerType: 2]
			def repo = codeManagementService.getRepo('', project, yaml.repository)
			def bDef = [name: yaml.name, project: yaml.project, repository: [id: repo.id, url: repo.url, type: 'TfsGit'], process: [yamlFilename: yaml.buildyaml, type:2], queue: queue, triggers:[trigger] ]
			if (yaml.variables) {
				bDef.variables = [:]
				yaml.variables.each { var ->
					if (!var.name) return
					bDef.variables[var.name] = [:]
					if (var.allowOverride) {
						bDef.variables[var.name].allowOverride = var.allowOverride
					}
					if (var.isSecret) {
						bDef.variables[var.name].isSecret = var.isSecret
					}
					if (var.'value') {
						bDef.variables[var.name].'value' = var.'value'
					}
					
				}
			}
			def query = ['api-version':'5.1']
			buildManagementService.writeBuildDefinition('', project, bDef, query)
		} else {
			def repo = codeManagementService.getRepo('', project, yaml.repository)
			def bDef = build
			bDef.repository = repo
			bDef.repository.type = 'TfsGit'
			bDef.process.yamlFilename = yaml.buildyaml
			bDef.queue = queue
			if (yaml.variables) {
				bDef.variables = [:]
				yaml.variables.each { var ->
					bDef.variables[var.name] = [:]
					if (var.allowOverride) {
						bDef.variables[var.name].allowOverride = var.allowOverride
					}
					if (var.isSecret) {
						bDef.variables[var.name].isSecret = var.isSecret
					}
					if (var.'value') {
						bDef.variables[var.name].'value' = var.'value'
					}
					
				}
			}
//			def bDef = [id: build.id, name: yaml.name, project: yaml.project, repository: [id: repo.id, url: repo.url, type: 'TfsGit'], process: [yamlFilename: yaml.buildyaml, type:2] ]
			//def query = ['api-version':'5.1']
			buildManagementService.updateBuildDefinition('', project, bDef)
			
		}
	}
}
