package com.zions.pipeline.services.cli.action.yaml;

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.yaml.YamlSlurper
import java.util.regex.Pattern
import java.util.regex.Matcher

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.code.CodeManagementService
import com.zions.pipeline.services.yaml.execution.YamlExecutionService
import com.zions.pipeline.services.yaml.template.execution.IExecutableYamlHandler
import groovy.util.logging.Slf4j


@Component
@Slf4j
class ExecuteYaml implements CliAction {
	@Autowired
	Map<String, IExecutableYamlHandler> yamlHandlerMap;
	
	@Autowired
	CodeManagementService codeManagementService
	
	@Autowired
	YamlExecutionService yamlExecutionService
	
	String[] pipelineFolders = ['.pipeline','pipeline']

	@Override
	public def execute(ApplicationArguments data) {
		//String[] repoUrls = data.getOptionValues('repoUrls')[0].split(',')
		//System.out.println("In ExecuteYaml - jsonStr:\n" + data.getOptionValues('jsonStr')[0])
		//String jsonStr = '['+data.getOptionValues('jsonStr')[0]+']'
		//String jsonStr = new JsonBuilder(data.getOptionValues('jsonStr')[0]).toPrettyString()
		//System.out.println("jsonStr before parseText:\n" + jsonStr)
		String localYaml = "false"
		if (data.getOptionValues('runlocal') != null) {
			localYaml = data.getOptionValues('runlocal')[0]
		}
		if (localYaml.toBoolean()) {
			System.out.println("Running local yaml ...")
			runExecutableYaml()
		} else {
			def commitsUrl = data.getOptionValues('commitsUrl')[0]
			//def adoData = new JsonSlurper().parseText(jsonStr)
			//if (adoData.resource && adoData.resource._links && adoData.resource._links.commits) {
				//def commitsUrl = "${adoData.resource._links.commits.href}"
				
				def commits = codeManagementService.getCommits(commitsUrl)
				def locations = getPipelineChangeLocations(commits)
				if (locations.size() > 0) {
					//String repoUrl = "${adoData.resource.repository.remoteUrl}"
					//String name = "${adoData.resource.repository.name}"
					String repoUrl = data.getOptionValues('repoUrl')[0]
					String name = data.getOptionValues('repoName')[0]
					yamlExecutionService.runExecutableYaml(repoUrl, name, locations)
				}
			//}
		}
		return null
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		//def required = ['commitsUrl','repoUrl','repoName']
		//required.each { name ->
		//	if (!args.containsOption(name)) {
		//		log.debug("Missing required argument:  ${name}.  Exiting ...")
		//		throw new Exception("Missing required argument:  ${name}")
		//	}
		//}
		return true
	}
	
	def getPipelineChangeLocations(def commits) {
		def locations = []
		for (def commit in commits.'value') {
			def changesUrl = "${commit._links.changes.href}"
			def changes = codeManagementService.getChanges(changesUrl)
			for (def change in changes.changes) {
				String path = "${change.item.path}"
				pipelineFolders.each { String pipelineFolder -> 
					if (path.contains("${pipelineFolder}") && path.endsWith('.yaml')) locations.add(path)
				}
			}
		}
		
		return locations
	}

	def runExecutableYaml() {
		def exeYaml = findExecutableYaml()
		for (def yaml in exeYaml) { 
			for (def exe in yaml.executables) {
				System.out.println("Loading handler for executable yaml of type: " + exe.type)				
				IExecutableYamlHandler yamlHandler = yamlHandlerMap[exe.type]
				if (yamlHandler) {
					System.out.println("Calling handler for executable yaml of type: " + exe.type)
					yamlHandler.handleYaml(exe, null, [])
				}
			}
		}
	}
	
	private def findExecutableYaml() {
		def executableYaml = []
		def filePattern = Pattern.compile("^.*[.]yaml\$")
		File outDir = new File(System.getProperty("user.dir"))
		System.out.println("Looking for executable yaml in: " + outDir.absolutePath)
		outDir.eachDirRecurse() { File dir ->
			dir.eachFileMatch(filePattern) { File file ->
				System.out.println("Checking file "+ file.getName() + " for executables")
				def eyaml = new YamlSlurper().parseText(file.text)
				def executables = eyaml.executables
				if (executables) {
					System.out.println("Found executable in file "+ file.getName())
					executableYaml.add(eyaml)
				}
			}
		}
		return executableYaml

	}
}