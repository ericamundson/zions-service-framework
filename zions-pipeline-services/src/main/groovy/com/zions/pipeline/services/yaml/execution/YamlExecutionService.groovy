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
	
	def runExecutableYaml(String repoUrl, String repoName, def scanLocations) {
		File dir = gitService.loadChanges(repoUrl, repoName)
		def exeYaml = findExecutableYaml(dir, scanLocations)
		for (def yaml in exeYaml) { 
			for (def exe in yaml.executables) {
								
				IExecutableYamlHandler yamlHandler = yamlHandlerMap[exe.type]
				if (yamlHandler) {
					yamlHandler.handleYaml(exe)
				}
			}
		}
	}
	
	private def findExecutableYaml(def repoDir, def scanLocations) {
		def executableYaml = []
		scanLocations.each { String loc ->
			File file = new File(repoDir, loc)
			def eyaml = new YamlSlurper().parseText(file.text)
			def executables = eyaml.executables
			if (executables) {
				executableYaml.add(eyaml)
			}
		}
		return executableYaml

	}
}
