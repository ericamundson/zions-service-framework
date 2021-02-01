package com.zions.pipeline.services.mixins

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchemaFactory
import com.zions.vsts.services.pullrequest.PullRequestManagementService
import groovy.yaml.YamlSlurper
import groovy.yaml.YamlBuilder
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import com.networknt.schema.SpecVersion
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.util.regex.Pattern
import java.util.regex.Matcher

trait FindExecutableYamlNoRepoTrait extends YamlTrait {
	@Value('${out.dir:}')
	File outDir
	
	@Value('${pipeline.folder:.pipeline}')
	String pipelineFolder
	
	def findExecutableYaml() {
		def executableYaml = []
		def filePattern = Pattern.compile("^.*[.]yaml\$")
		File pipelineDir = new File(outDir, pipelineFolder)
		if (!pipelineDir.exists()) return
		pipelineDir.eachDirRecurse() { File dir ->
			dir.eachFileMatch(filePattern) { File file ->
				def eyaml = null
				try {
					eyaml = new YamlSlurper().parseText(file.text)
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
							executableYaml.add(eyaml)
						} else {
							def feedback = [messages: oinvalidMessages]
							sendFeedback(feedback)
						}
					}
				}
			}
		}
		return executableYaml

	}	
	def sendFeedback(def feedback) {
		String err = errorMessage(feedback)
		System.err.println err
	}
	String errorMessage(def commentData) {
		String out = ""
		if (commentData.location) {
			out << "${commentData.location}\n"
		}
		for (String message in commentData.messages) {
			if (commentData.location) {
				out << "\t${message}\n"
			} else {
				out << "${message}\n"
				
			}
		}
		return out
	}


}
