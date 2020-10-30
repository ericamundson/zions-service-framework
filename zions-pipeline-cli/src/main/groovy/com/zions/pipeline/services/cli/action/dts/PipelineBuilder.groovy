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

/**
 * Command line action to execute blueprint and run executable yaml.
 * 
 * <p><b>Command-line arguments:</b></p>
 * <ul>
 * 	<li>pipelineBuilder - The action's Spring bean name.</li>
 * <ul>
 * <p><b>The following's command-line format: --name=value</b></p>
 * <ul>
 *  <li>blueprint.dir - Local directory location for XL blueprints</li>
 *  <li>blueprint - name of blueprint</li>
 *  <li>repo.dir - GIT repository directory</li>
 *  <li>out.dir - XL blueprint output directory</li>
 *  <li>ado.project - ADO project name</li>
 *  </ul>
 * </ul>
 * 
 * @author z091182
 * @startuml PipelineBuilder_components.svg
 * actor RE as "Release Engineer"
 * artifact zions-pipeline-cli.jar {
 *   package dts as "com.zions.pipeline.services.cli.action.dts" {
 *     component PipelineBuilder as "PipelineBuilder"
 *   }
 *   RE --> PipelineBuilder: executes
 * }
 * 
 * artifact zions-pipeline-services.jar {
 *    package template as "com.zions.pipeline.services.yaml.template" {
 *      component BlueprintInterpretorService
 *    }
 *    package execution as "com.zions.pipeline.services.yaml.template.execution" {
 *      interface IExecutableYamlHandler 
 *      component BranchPolicy
 *      component BuildDefinition
 *      component GitRepository
 *      component RunXLBlueprints
 *      component RunXLDeployApply
 *      component RunXLReleaseApply
 *      component SanitizeProperties
 *      component WebHookSubscriptions
 *      component WorkItem
 *      BlueprintInterpretorService --> IExecutableYamlHandler: calls
 *      BranchPolicy -[dotted]-> IExecutableYamlHandler: implements
 *      BuildDefinition -[dotted]-> IExecutableYamlHandler: implements
 *      GitRepository -[dotted]-> IExecutableYamlHandler: implements
 *      RunXLBlueprints -[dotted]-> IExecutableYamlHandler: implements
 *      RunXLDeployApply -[dotted]-> IExecutableYamlHandler: implements
 *      RunXLReleaseApply -[dotted]-> IExecutableYamlHandler: implements
 *      SanitizeProperties -[dotted]-> IExecutableYamlHandler: implements
 *      WebHookSubscriptions -[dotted]-> IExecutableYamlHandler: implements
 *      WorkItem -[dotted]-> IExecutableYamlHandler: implements
 *    }
 *    PipelineBuilder --> BlueprintInterpretorService: Execute blueprint and run executable yaml
 * }
 * 
 * 
 * @enduml
 * 
 */
@Component
@Slf4j
class PipelineBuilder implements CliAction {
	
	@Autowired
	BlueprintTemplateInterpretorService blueprintTemplateInterpretorService
	
	@Value('${run.remote:false}')
	Boolean runRemote

	public def execute(ApplicationArguments data) {
		//def answers = blueprintTemplateInterpretorService.loadAnswers()
		//println "Answers:  ${answers}"
		blueprintTemplateInterpretorService.outputPipeline()
		if (!runRemote) {
			blueprintTemplateInterpretorService.runExecutableYaml()
		} else {
			blueprintTemplateInterpretorService.runPullRequestOnChanges()
		}
	}
	
	
	public Object validate(ApplicationArguments args) throws Exception {
		
	}
}
