package com.zions.pipeline.services.cli.action.blueprint

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

import com.zions.common.services.cli.action.CliAction
import com.zions.pipeline.services.git.GitService
import com.zions.vsts.services.extdata.ExtensionDataManagementService

import groovy.yaml.YamlSlurper
import groovy.yaml.YamlBuilder
import groovy.json.JsonBuilder
import org.yaml.snakeyaml.Yaml

import com.zions.pipeline.services.blueprint.model.*

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
 * }
 * RE --> PipelineBuilder: executes
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
 *      RunXLDeployApply -[dotted]up-> IExecutableYamlHandler: implements
 *      RunXLReleaseApply -[dotted]up-> IExecutableYamlHandler: implements
 *      SanitizeProperties -[dotted]up-> IExecutableYamlHandler: implements
 *      WebHookSubscriptions -[dotted]up-> IExecutableYamlHandler: implements
 *      WorkItem -[dotted]do-> IExecutableYamlHandler: implements
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
class SendAdoBlueprintData implements CliAction {
	
	@Autowired
	GitService gitService
	
	@Autowired
	ExtensionDataManagementService extensionDataManagementService

	
	@Value('${blueprint.repo.urls:}')
	String[] blueprintRepoUrls

	public def execute(ApplicationArguments data) {
		String extKey = 'blueprint_execute_data'
		List<Folder> repositories = []
		Map<String, Folder> parentMap = [:]
		Set<String> repoNames = []
		for (String repoUrl in blueprintRepoUrls) {
			File repo = gitService.loadChanges(repoUrl)
			int nIndex = repoUrl.lastIndexOf('/')+1
			String repoName = repoUrl.substring(nIndex)
			repoNames.add(repoName)
			Folder arepo = new Folder([name: repoName])
			parentMap[repoName] = arepo
			repositories.add(arepo)
			repo.eachDirRecurse() { File dir ->
				//if (dir.name == '.git') return
				dir.eachFile { File file ->
					if (file.name == 'blueprint.yaml') {
						File parentFile = file.parentFile
						String parentName = parentFile.name
						
						Blueprint blueprint = new Blueprint([name: parentName, repoUrl: repoUrl, outDir: []])
						String bText = file.text
						bText = bText.replaceAll('!expr', '-expr')
						def byaml = new YamlSlurper().parseText(bText)
						Map answers = [:]
						def confirms = [:]
						def outDirYaml = null
						File outDirYamlFile = new File(parentFile, 'outDir.yaml')
						if (outDirYamlFile.exists()) {
							outDirYaml = new YamlSlurper().parseText(outDirYamlFile.text)
							blueprint.outDir = outDirYaml.outdir
						}
						File outRepoYamlFile = new File(parentFile, 'outRepo.yaml')
						if (outRepoYamlFile.exists()) {
							def outRepoYaml = new YamlSlurper().parseText(outRepoYamlFile.text)
							blueprint.outRepoName = outRepoYaml.outRepo.name
						}
						File permissionsFile = new File(parentFile, 'permissions.yaml')
						if (permissionsFile.exists()) {
							def perissionsYaml = new YamlSlurper().parseText(permissionsFile.text)
							blueprint.permissions = perissionsYaml.permissions
						}
						for (def parm in byaml.spec.parameters) {
							if (!parm.value) {
//								if (parm.promptIf) {
//									String test = parm.promptIf
//									test = test.substring(test.indexOf('-expr')+5)
//									test = test.trim()
//									String[] testParts = test.split('"')
//								}
								Parameter question = new Parameter(name: parm.name, type: parm.type, adefault: parm.default, description: parm.description, label: parm.label, options: parm.options, prompt: parm.prompt, promptIf: parm.promptIf, validate: parm.validate)
								blueprint.parameters.add(question)
							}
						}
						
						Folder folder = new Folder([name: parentName])
						if (!parentMap[parentName]) {
						    folder = new Folder([name: parentName])
							parentMap[parentName] = folder
						} else {
							folder = parentMap[parentName]
						}
						folder.blueprints.add(blueprint)
						while (folder.name != repoName) {
							parentFile = parentFile.parentFile
							parentName = parentFile.name
							Folder parent = null
							if (!parentMap[parentName]) {
							    parent = new Folder([name: parentName])
								parentMap[parentName] = parent
							} else {
								parent = parentMap[parentName]
							}
							if (!parent.folders.contains(folder)) {
								parent.folders.add(folder)
							}
							folder.parentName = parent.name
							folder = parent
						}
					}
				}
			}
		}
		
		repositories = addBPPaths(repositories, parentMap, repoNames)
		
		def extData = [id: extKey, repositories: repositories]
		
		String json = new JsonBuilder(extData).toPrettyString()
		log.info(json)
		extensionDataManagementService.ensureExtensionData(extData)
		
	}
	
	def addBPPaths( List<Folder> respositories, Map<String, Folder> parentMap, Set<String> repoNames) {
		parentMap.each { String name, Folder folder ->
			if (folder.blueprints.size() > 0) {
				String parentName = folder.parentName
				String path = "${parentName}"
				while (parentName != null) {
					Folder parent = parentMap[parentName]
					parentName = parent.parentName
					if (repoNames.contains(parentName)) break
					path = "${parentName}/${path}"
				}
				for (Blueprint bp in folder.blueprints) {
					bp.path = path
				}
			}
		}
		return respositories
	}
	
	def cleanUp(Folder folder, Folder parent) {
		if (parent && folder.folders.size() == 0 && folder.blueprints.size() == 0) {
			parent.folders.remove(folder)
		}
		for (Folder child in folder.folders) {
			cleanUp(child, folder)
		}
	}
	
	public Object validate(ApplicationArguments args) throws Exception {
		
	}
}
