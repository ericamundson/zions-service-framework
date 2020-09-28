package com.zions.pipeline.services.yaml.execution
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import groovy.yaml.YamlSlurper
import groovy.yaml.YamlBuilder
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j

import org.yaml.snakeyaml.Yaml
import java.util.regex.Pattern
import java.util.regex.Matcher

import com.zions.pipeline.services.git.GitService
import com.zions.pipeline.services.yaml.template.execution.IExecutableYamlHandler
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.pullrequest.PullRequestManagementService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.zions.pipeline.services.mixins.FindExecutableYamlTrait


@Component
@Slf4j
class YamlExecutionService implements FindExecutableYamlTrait {
	@Autowired
	Map<String, IExecutableYamlHandler> yamlHandlerMap;
	
	@Autowired
	GitService gitService
	
	@Autowired
	CodeManagementService codeManagementService
	
	@Autowired
	ProjectManagementService projectManagementService
	
	
	@Value('${pipeline.folders:.pipeline,pipeline}')
	String[] pipelineFolders

	@Value('${always.execute.folder:executables}')
	String alwaysExecuteFolder
			

	def runExecutableYaml(String repoUrl, String repoName, def scanLocations, String branch, String project, String pullRequestId = null) {
		File repo = null
		def projectData = projectManagementService.getProject('', project)
		def repoData = codeManagementService.getRepo('', projectData, repoName)
		try {
			repo = gitService.loadChanges(repoUrl, repoName, branch)
		} catch (e) {
			StringWriter sw = new StringWriter()
			PrintWriter pw = new PrintWriter(sw)
			e.printStackTrace(pw);
			String issue = sw
			Set issues = [[message: issue]]
			log.error('Failed to update GIT repo. Error: '+e.getMessage())
			def feedback = [ messages: issues]
			sendFeedback(projectData, repoData, feedback, pullRequestId)
			repo = null
		}
		if (!repo) return
		def exeYaml = findExecutableYaml(repo, scanLocations, projectData, repoData, pullRequestId)
		for (def yamldata in exeYaml) { 
			if (yamldata.yaml.executables) {
				Set issues = []
				for (def exe in yamldata.yaml.executables) {
									
					IExecutableYamlHandler yamlHandler = yamlHandlerMap[exe.type]
					if (yamlHandler) {
						try {
							yamlHandler.handleYaml(exe, repo, scanLocations, branch, project)
						} catch (e) {
							StringWriter sw = new StringWriter()
							PrintWriter pw = new PrintWriter(sw)
							e.printStackTrace(pw);
							pw.close()
							String issue = sw
							issues.add("${exe.type} error: ${issue}")
						}
					}
				}
				if (!issues.empty) {
					def feedback = [location: yamldata.location, messages: issues]
					sendFeedback(projectData, repoData, feedback, pullRequestId)
				}
			}
		}
		gitService.reset(repo)
	}
	
	
	

}
