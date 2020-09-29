package com.zions.pipeline.services.mixins

import org.springframework.beans.factory.annotation.Autowired

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchemaFactory
import com.zions.vsts.services.pullrequest.PullRequestManagementService
import groovy.yaml.YamlSlurper
import groovy.yaml.YamlBuilder
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import com.networknt.schema.SpecVersion
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory

trait FindExecutableYamlTrait {
	@Autowired(required=false)
	PullRequestManagementService pullRequestManagementService
	
	Map<String, String> schemaCache = [:]
	
	ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
	
	JsonSchemaFactory factory = JsonSchemaFactory.builder(
		JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
	)
	.objectMapper(mapper)
	.build()

	def findExecutableYaml(File repoDir, def scanLocations, def projectData, def repoData, String pullRequestId = null) {
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
						sendFeedback(projectData, repoData, feedback, pullRequestId)
					}
				}
			}
		}
		
		return executableYaml

	}
	
	def sendFeedback(def projectData, def repoData, def feedback, String pullRequestId = null) {
		if (pullRequestId) {
			pullRequestManagementService.createdCommentThread('', projectData, repoData, pullRequestId, feedback)
		} else {
			String err = errorMessage(feedback)
			if (log) {
				log.error(err)
			} else {
				System.err.println err
			}
		}
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

	def validateYaml(String yaml, String version, String type) {
		String schema = null
		if (schemaCache.containsKey(version)) {
			schema = schemaCache[version]
		} else {
			schema = loadSchema(version)
			if (schema) {
				schema = schema.replace('\t', '  ')
			}
			schemaCache[version] = schema
		}
		if (!schema) {
			return ["${type}: No schema defined to validate yaml!"]
		}
		Set invalidMessages = factory.getSchema(schema).validate(mapper.readTree(yaml))
		Set outmessages = []
		if (!invalidMessages.empty) {
			for (String imessage in invalidMessages) {
				outmessages.add("${type}: ${imessage}")
				//log.error("${type}: ${imessage}")
			}
		}
		return outmessages
	}
	
	String loadSchema(String version) {
		InputStream is = this.getClass().getResourceAsStream("/${version}.json")
		
		if (!is) {
			URL url = this.getClass().getResource("/${version}.json")
			if (url) {
				File f = new File(url.toURI())
				return f.text
			}
			return null
		}
		return is.text
	}

}
