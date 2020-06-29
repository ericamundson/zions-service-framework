package com.zions.pipeline.services.cli.action.dts

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

import com.zions.common.services.cli.action.CliAction
import com.zions.pipeline.services.yaml.template.BlueprintTemplateInterpretorService
import groovy.yaml.YamlSlurper
import groovy.yaml.YamlBuilder
import groovy.json.JsonBuilder
import org.yaml.snakeyaml.Yaml

import groovy.util.logging.Slf4j

@Component
@Slf4j
class PipelineBuilder implements CliAction {
	
	@Autowired
	BlueprintTemplateInterpretorService blueprintTemplateInterpretorService

	public def execute(ApplicationArguments data) {
		def answers = blueprintTemplateInterpretorService.loadAnswers()
		println "Answers:  ${answers}"
		blueprintTemplateInterpretorService.outputPipeline(answers)
		blueprintTemplateInterpretorService.runExecutableYaml()
	}
	
	
	public Object validate(ApplicationArguments args) throws Exception {
		
	}
}
