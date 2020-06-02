package com.zions.pipeline.services.yaml.template

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import groovy.yaml.YamlSlurper
import groovy.yaml.YamlBuilder
import groovy.json.JsonBuilder
import org.yaml.snakeyaml.Yaml

import groovy.util.logging.Slf4j
import java.util.regex.Pattern
import java.util.regex.Matcher

import com.zions.pipeline.services.yaml.template.execution.IExecutableYamlHandler


@Component
class BlueprintTemplateInterpretorService {
	
	@Autowired
	Map<String, IExecutableYamlHandler> yamlHandlerMap;
	
	@Value('${blueprint.dir:}')
	File blueprintDir
	
	@Value('${blueprint:}')
	String blueprint
	
	@Value('${out.dir:}')
	File outDir
	@Value('${in.placeholder.delimiters:[[,]]}')
	String[] inDelimiters
	
	def answers = [:]
	
	Map loadAnswers() {
		File blueprint = new File("${blueprintDir}/${blueprint}/blueprint.yaml")
		String bText = blueprint.text
		def byaml = new YamlSlurper().parseText(bText)
		Map answers = [:]
		def confirms = [:]
		byaml.spec.parameters.each { parm ->
			String type = "${parm.type}"
			String promptIf = parm.promptIf
			if (type == 'Confirm') {
				print "${parm.prompt} "
				answers[parm.name] = false
				confirms[parm.name] = System.in.newReader().readLine()
				if (confirms[parm.name].toLowerCase() == 'y') {
					answers[parm.name] = true
				}
			} else if (promptIf == null || (promptIf && confirms[promptIf].toLowerCase() == 'y')) {
				print "${parm.prompt} "
				answers[parm.name] = System.in.newReader().readLine()
			}
		}
		return answers
	}
	
	def outputPipeline(Map answers) {
		//initialize pipeline dir
		new AntBuilder().copy( todir: "${outDir}/pipeline", overwrite: true ) {
			fileset( dir: "${blueprintDir}/${blueprint}" ) {
					include( name: "xlw.bat")
					include( name: "xlw")
					include( name: ".xebialabs/**/*")
				}
		}
		//write answers file.
		def answersOut = new YamlBuilder()
		answersOut.call(answers)
		String answersStr = answersOut.toString()
		File pipelineDir = new File(outDir, 'pipeline')
		File answersFile = new File(pipelineDir, 'answers.yaml')
		def os = answersFile.newDataOutputStream()
		os << answersStr
		os.close()
		
		//Generate pipeline
		new AntBuilder().exec(dir: "${outDir}/pipeline", executable: 'cmd', failonerror: true) {
			arg( line: "/c xlw.bat blueprint -a ${outDir}/pipeline/answers.yaml -l ${blueprintDir} -b ${blueprint} -s")
		}
		
		//fix placeholders.
		new AntBuilder().replace(dir: "${outDir}/pipeline") {
			replacefilter( token: "${inDelimiters[0]}", value: '{{')
			replacefilter( token: "${inDelimiters[1]}", value: '}}')
		}

	}
	
	def runExecutableYaml() {
		def exeYaml = findExecutableYaml()
		for (def yaml in exeYaml) { 
			for (def exe in yaml.executables) {
								
				IExecutableYamlHandler yamlHandler = yamlHandlerMap[exe.type]
				if (yamlHandler) {
					yamlHandler.handleYaml(exe)
				}
			}
		}
	}
	
	private def findExecutableYaml() {
		def executableYaml = []
		def filePattern = Pattern.compile("^.*[.]yaml\$")
		outDir.eachDirRecurse() { File dir ->
			dir.eachFileMatch(filePattern) { File file ->
				def eyaml = new YamlSlurper().parseText(file.text)
				def executables = eyaml.executables
				if (executables) {
					executableYaml.add(eyaml)
				}
			}
		}
		return executableYaml

	}
}
