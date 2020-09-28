package com.zions.pipeline.services.yaml.template

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import groovy.yaml.YamlSlurper
import groovy.yaml.YamlBuilder
import groovy.json.JsonBuilder
import org.yaml.snakeyaml.Yaml
import java.util.regex.Pattern
import java.util.regex.Matcher

import com.zions.pipeline.services.mixins.FindExecutableYamlNoRepoTrait
import groovy.util.logging.Slf4j

import com.zions.pipeline.services.yaml.template.execution.IExecutableYamlHandler


@Component
@Slf4j
class BlueprintTemplateInterpretorService implements  FindExecutableYamlNoRepoTrait {
	
	@Autowired
	Map<String, IExecutableYamlHandler> yamlHandlerMap;
	
	@Value('${blueprint.dir:}')
	File blueprintDir
	
	@Value('${blueprint:}')
	String blueprint
		
	@Value('${repo.dir:}')
	File repoDir
	
	@Value('${out.dir:}')
	File outDir

	@Value('${pipeline.folder:.pipeline}')
	String pipelineFolder
	
	@Value('${in.placeholder.delimiters:[[,]]}')
	String[] inDelimiters
	
	@Value('${ado.project:DTS}')
	String adoProject
	
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
	
	def outputPipeline() {
		//initialize pipeline dir
		loadXLCli()
		//write answers file.
//		def answersOut = new YamlBuilder()
//		answersOut.call(answers)
//		String answersStr = answersOut.toString()
		File pipelineDir = new File(outDir, pipelineFolder)
		if (!pipelineDir.exists()) {
			pipelineDir.mkdirs()
		}
		File startupBat = new File(pipelineDir, 'startup.bat')
		def os = startupBat.newDataOutputStream()
		os << 'start /W cmd /C %*'
		os.close()
//		File answersFile = new File(pipelineDir, 'answers.yaml')
//		def os = answersFile.newDataOutputStream()
//		os << answersStr
//		os.close()
		String osname = System.getProperty('os.name')
		
		//Generate pipeline
		if (osname.contains('Windows')) {
			new AntBuilder().exec(dir: "${outDir}/${pipelineFolder}", executable: "${outDir}/${pipelineFolder}/startup.bat", failonerror: true) {
//				arg( line: "/c ${outDir}/${pipelineFolder}/xl blueprint -a ${outDir}/pipeline/answers.yaml -l ${blueprintDir} -b \"${blueprint}\" -s")
				arg( line: "${outDir}/${pipelineFolder}/xl blueprint  -l ${blueprintDir} -b \"${blueprint}\" ")
			}
		} else {
			new AntBuilder().exec(dir: "${outDir}/${pipelineFolder}", executable: '/bin/sh', failonerror: true) {
				arg( line: "-c ${outDir}/${pipelineFolder}/xl blueprint -a ${outDir}/pipeline/answers.yaml -l ${blueprintDir} -b \"${blueprint}\" -s")
			}

		}
		
		//fix placeholders.
//		new AntBuilder().replace(dir: "${outDir}/${pipelineFolder}") {
//			replacefilter( token: "${inDelimiters[0]}", value: '{{')
//			replacefilter( token: "${inDelimiters[1]}", value: '}}')
//		}

	}
	
	def runExecutableYaml() {
		def exeYaml = findExecutableYaml()
		for (def yaml in exeYaml) { 
			for (def exe in yaml.executables) {
								
				IExecutableYamlHandler yamlHandler = yamlHandlerMap[exe.type]
				if (yamlHandler) {
					try {
						yamlHandler.handleYaml(exe, repoDir, [], 'refs/heads/master', adoProject)
					} catch (e) {
						log.error("Failed running executable yaml:  ${exe.type} :: ${e.message}")
						e.printStackTrace()
					}
				}
			}
		}
	}
	
	
	def loadXLCli() {
		String osname = System.getProperty('os.name')
			
		if (osname.contains('Windows')) {
			InputStream istream = this.getClass().getResourceAsStream('/xl/windows/xl.exe')
			File pipelineDir = new File(outDir, pipelineFolder)
			if (!pipelineDir.exists()) {
				pipelineDir.mkdirs()
			}
			File of = new File(pipelineDir, 'xl.exe')
			def aos = of.newDataOutputStream()
			aos << istream
			aos.close()
		} else {
			InputStream istream = this.getClass().getResourceAsStream('/xl/linux/xl')
			File pipelineDir = new File(outDir, pipelineFolder)
			if (!pipelineDir.exists()) {
				pipelineDir.mkdirs()
			}
			File of = new File(pipelineDir, 'xl')
			def aos = of.newDataOutputStream()
			aos << istream
			aos.close()

		}
	}
}
