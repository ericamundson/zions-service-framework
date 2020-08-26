package com.zions.pipeline.services.yaml.execution
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import groovy.yaml.YamlSlurper
import groovy.yaml.YamlBuilder
import groovy.json.JsonBuilder
import org.yaml.snakeyaml.Yaml
import java.util.regex.Pattern
import java.util.regex.Matcher

import com.zions.pipeline.services.git.GitService
import com.zions.pipeline.services.yaml.template.execution.IExecutableYamlHandler

@Component
class YamlExecutionService {
	@Autowired
	Map<String, IExecutableYamlHandler> yamlHandlerMap;
	
	@Autowired
	GitService gitService
	
	@Value('${pipeline.folders:.pipeline,pipeline}')
	String[] pipelineFolders

	@Value('${always.execute.folder:executables}')
	String alwaysExecuteFolder

	def runExecutableYaml(String repoUrl, String repoName, def scanLocations, String branch) {
		File repo = gitService.loadChanges(repoUrl, repoName, branch)
		def exeYaml = findExecutableYaml(repo, scanLocations)
		for (def yaml in exeYaml) { 
			if (yaml.executables) {
				for (def exe in yaml.executables) {
									
					IExecutableYamlHandler yamlHandler = yamlHandlerMap[exe.type]
					if (yamlHandler) {
						yamlHandler.handleYaml(exe, repo, scanLocations, branch)
					}
				}
			}
		}
	}
	
	private def findExecutableYaml(File repoDir, def scanLocations) {
		def executableYaml = []
		scanLocations.each { String loc ->
			File file = new File(repoDir, loc)
			def eyaml = new YamlSlurper().parseText(file.text)
			def executables = eyaml.executables
			if (executables) {
				executableYaml.add(eyaml)
			}
		}
		List<String> pFolders = Arrays.asList(pipelineFolders)
		repoDir.eachDirRecurse() { File d ->
			String dname = d.name
			if (pFolders.contains(dname)) {
				File executables = new File(d, alwaysExecuteFolder)
				if (executables.exists()) {
					executables.eachFile() { File eFile ->
						String name = eFile.name
						if (name.endsWith('.yaml')) {
							def eyaml = new YamlSlurper().parseText(eFile.text)
							executableYaml.add(eFile)
						}
					}
				}

			}
		}
//		pipelineFolders.each { String pipelineFolder ->
//			File pipelineDir = new File(repoDir, pipelineFolder)
//			if (pipelineDir.exists()) {
//				File executables = new File(pipelineDir, alwaysExecuteFolder)
//				if (executables.exists()) {
//					executables.eachFile() { File eFile ->
//						String name = eFile.name
//						if (name.endsWith('.yaml')) {
//							def eyaml = new YamlSlurper().parseText(eFile.text)
//							executableYaml.add(eFile)
//						}
//					}
//				}
//			}
//		}
		return executableYaml

	}
}
