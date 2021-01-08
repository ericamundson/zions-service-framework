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
			if (repo && repo.exists()) {
				int nIndex = repoUrl.lastIndexOf('/')+1
				String repoName = repoUrl.substring(nIndex)
				repoNames.add(repoName)
				Folder arepo = new Folder([name: repoName])
				parentMap[repo.absolutePath] = arepo
				repositories.add(arepo)
				repo.eachDirRecurse() { File dir ->
					//if (dir.name == '.git') return
					dir.eachFile { File file ->
						if (file.name == 'blueprint.yaml') {
							File parentFile = file.parentFile
							String parentName = parentFile.name
							String parentPath = parentFile.absolutePath
							String opath = parentPath.substring(repo.absolutePath.length()+1)
							//println "opath: ${opath}, parentName: ${parentName}"
							if (opath != parentName) {
								opath = opath.substring(0, opath.length() - parentName.length()-1)
								opath = opath.replace('\\', '/')
							}
							Blueprint blueprint = new Blueprint([name: parentName, repoUrl: repoUrl, outDir: [], path:opath])
							String bText = file.text
							bText = fixYamlForParse(bText)
							def byaml = new YamlSlurper().parseText(bText)
							blueprint.description = byaml.metadata.description
							blueprint.title = byaml.metadata.name
							Map answers = [:]
							def confirms = [:]
							def selfserveYaml = null
							File selfserveYamlFile = new File(parentFile, 'selfserve.yaml')
							if (selfserveYamlFile.exists()) {
								selfserveYaml = new YamlSlurper().parseText(selfserveYamlFile.text)
								if (selfserveYaml.outdir) {
									blueprint.outDir = selfserveYaml.outdir
								}
								if (selfserveYaml.outRepo) {
									blueprint.outRepoName = selfserveYaml.outRepo.name
								}
								if (selfserveYaml.permissions) {
									blueprint.permissions = selfserveYaml.permissions
								}
								if (selfserveYaml.selectedProjectParm) {
									blueprint.selectedProjectParm = selfserveYaml.selectedProjectParm.bpname
								}
							}
							
							handleInclude('includeBefore', blueprint.parameters, byaml, repo)
							for (def parm in byaml.spec.parameters) {
								if (!parm.value) {
									//								if (parm.promptIf) {
									//									String test = parm.promptIf
									//									test = test.substring(test.indexOf('-expr')+5)
									//									test = test.trim()
									//									String[] testParts = test.split('"')
									//								}
									Parameter question = new Parameter(name: parm.name, type: parm.type, adefault: parm.default, description: parm.description, label: parm.label, options: fixExprForUse(parm.options), prompt: parm.prompt, promptIf: parm.promptIf, validate: fixExprForUse(parm.validate))
									blueprint.parameters.add(question)
								}
							}

							handleInclude('includeAfter',blueprint.parameters, byaml, repo)


							Folder folder = new Folder([name: parentName, title: parentName])
							if (!parentMap[parentPath]) {
								folder = new Folder([name: parentName, title: parentName])
								parentMap[parentPath] = folder
							} else {
								folder = parentMap[parentPath]
							}
							folder.blueprints.add(blueprint)
							while (folder.name != repoName) {
								parentFile = parentFile.parentFile
								parentName = parentFile.name
								parentPath = parentFile.absolutePath
								Folder parent = null
								if (!parentMap[parentPath]) {
									parent = new Folder([name: parentName, title: parentName])
									parentMap[parentPath] = parent
								} else {
									parent = parentMap[parentPath]
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
		}

		//repositories = addBPPaths(repositories, parentMap, repoNames)

		def extData = [id: extKey, repositories: repositories]

		String json = new JsonBuilder(extData).toPrettyString()

		//String[] parts = "\"a bunch of stuff\"".split('"')
		log.info(json)
		extensionDataManagementService.ensureExtensionData(extData)

	}


	def handleInclude(String iType, def parms, def byaml, File repo) {
		if (!byaml.spec[iType]) return;
		for (def frag in byaml.spec[iType]) {
			File fragFile = new File(repo, "${frag.blueprint}/blueprint.yaml")
			if (fragFile.exists()) {
				String bText = fragFile.text
				bText = fixYamlForParse(bText)
				def fyaml = new YamlSlurper().parseText(bText)
				def poverides = frag.parameterOverrides
				Set overrides = []
				if (poverides) {
					for (def override in poverides) {
						overrides.add(override.name)
					}
				}
				def pPromptIf = frag.includeIf
				if (pPromptIf && pPromptIf.indexOf('-expr') == -1) {
					pPromptIf = "-expr \"${pPromptIf}\""
				}
				for (def parm in fyaml.spec.parameters) {
					if (!parm.value && !overrides.contains(parm.name)) {
						String promptIf = parm.promptIf
						if (pPromptIf) {
							promptIf = buildPromptIf(promptIf, pPromptIf)
						}
						if (promptIf) {
							promptIf = fixExprForUse(promptIf)
						}
						Parameter question = new Parameter(name: parm.name, type: parm.type, adefault: parm.default, description: parm.description, label: parm.label, options: fixExprForUse(parm.options), prompt: parm.prompt, promptIf: promptIf, validate: fixExprForUse(parm.validate))
						parms.add(question)
					}
				}

			}
		}

	}
	
	String fixYamlForParse(String ins) {
		ins = ins.replaceAll('!expr', '-expr')
		
		def items = (ins =~ /-expr\s+["].+["]/).findAll()
		
		for (String item in items) {
			String citem = item.replace(':', '%3A')
			ins = ins.replace(item, citem)
		}
		return ins
	}
	
	def fixExprForUse(def ins) {
		if (!ins) return
		if (ins instanceof String) {
			ins = ins.replaceAll('%3A', ':')
			
			return ins
		} else if (ins instanceof String[]) {
			for (String i in ins) {
				i = i.replaceAll('%3A', ':')
			}
			return ins
		} else if (ins instanceof Object[] && ins.size() > 0 && ins[0].hasProperty('value')) {
			for (def i in ins) {
				i.value = i.value.replaceAll('%3A', ':')
			}
			return ins
		}
		//ins = ins.replaceAll('%3A', ':')
		return ins
	}

	String buildPromptIf(String promptIf, String pPromptIf) {
		String outPromptIf = ''
		if (promptIf) {
			String cexpr = getExpr(promptIf)
			String pexpr = getExpr(pPromptIf)
			return "-expr \"(${pexpr}) && (${cexpr})\""
		} else {
			return pPromptIf
		}
		return ''
	}

	String getExpr(String expr) {
		expr = expr.substring(expr.indexOf('-expr') + 5)
		expr = expr.trim()
		expr = expr.replace('"', '')
		return expr
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
