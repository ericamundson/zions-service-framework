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


@Component
@Slf4j
class YamlExecutionService {
	@Autowired
	Map<String, IExecutableYamlHandler> yamlHandlerMap;
	
	@Autowired
	GitService gitService
	
	@Autowired
	CodeManagementService codeManagementService
	
	@Autowired
	ProjectManagementService projectManagementService
	
	@Autowired
	PullRequestManagementService pullRequestManagementService
	
	@Value('${pipeline.folders:.pipeline,pipeline}')
	String[] pipelineFolders

	@Value('${always.execute.folder:executables}')
	String alwaysExecuteFolder
	
	Map<String, String> schemaCache = [:]
	
	ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
	
	JsonSchemaFactory factory = JsonSchemaFactory.builder(
		JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
	)
	.objectMapper(mapper)
	.build()
	

	def runExecutableYaml(String repoUrl, String repoName, def scanLocations, String branch, String project, String pullRequestId) {
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
			sendFeedback(projectData, repoData, pullRequestId, feedback)
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
							String issue = sw
							issues.add("${exe.type} error: ${issue}")
						}
					}
				}
				if (!issues.empty) {
					def feedback = [location: yamldata.location, messages: issues]
					sendFeedback(projectData, repoData, pullRequestId, feedback)
				}
			}
		}
		gitService.close(repo)
	}
	
	private def findExecutableYaml(File repoDir, def scanLocations, def projectData, def repoData, String pullRequestId) {
		def executableYaml = []
		scanLocations.each { String loc ->
			File file = new File(repoDir, loc)
			String outStr = file.text
			outStr = outStr.replaceAll(/(#)( |\S)*$/, '')
			def eyaml = null
			try {
				eyaml = new YamlSlurper().parseText(outStr)
			} catch (e) {}
			if (eyaml) {
				def executables = eyaml.executables
				if (executables) {
					boolean valid = true
					Set oinvalidMessages = []
					for (def executable in executables) {
						String type = executable.type
						String version = "${type}_v1"
						if (type.indexOf('/') != -1) {
							String v = type.substring(type.indexOf('/')+1)
							if (v == 'v1') {
								executable.type = type.substring(0, type.indexOf('/'))
							} else {
								executable.type = type.replace('/', '_')
							}
							version = "${type}_${v}"
						}
						YamlBuilder yb = new YamlBuilder()
						
						yb( executable )
						
						String yaml = yb.toString()
		
						Set invalidMessages = validateYaml(yaml, version, executable.type)
						oinvalidMessages.addAll(invalidMessages)
						
					}
					if (oinvalidMessages.empty) {
						executableYaml.add([location: loc, yaml: eyaml])
					} else {
						def feedback = [location: loc, messages: oinvalidMessages]
						sendFeedback(projectData, repoData, pullRequestId, feedback)
					}
				}
			}
		}
		
		return executableYaml

	}
	
	def sendFeedback(def projectData, def repoData, String pullRequestId, def feedback) {
		pullRequestManagementService.createdCommentThread('', projectData, repoData, pullRequestId, feedback)
	}
	
	def validateYaml(String yaml, String version, String type) {
		String schema = null
		if (schemaCache.containsKey(version)) {
			schema = schemaCache[version]
		} else {
			schema = loadSchema(version)
			schema = schema.replace('\t', '  ')
			schemaCache[version] = schema
		}
		if (!schema) return []
		Set invalidMessages = factory.getSchema(schema).validate(mapper.readTree(yaml))
		Set outmessages = []
        if (!invalidMessages.empty) {
			for (String imessage in invalidMessages) {
				outmessages.add("${type}: ${imessage}")
				log.error("${type}: ${imessage}")
			}
		}
		return outmessages    
	}

	String loadSchema(String version) {
		URL url = this.getClass().getResource("/${version}.json")
		File schemaFile = new File(url.file)
		return schemaFile.text
	}
}
