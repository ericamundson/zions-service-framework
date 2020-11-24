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
import com.zions.pipeline.services.mixins.XLCliTrait

import groovy.util.logging.Slf4j

import com.zions.pipeline.services.yaml.template.execution.IExecutableYamlHandler
import com.zions.pipeline.services.git.GitService
import com.zions.vsts.services.policy.PolicyManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.admin.member.MemberManagementService


/**
 * Performs the following behaviors:
 * o Runs a XL Blueprint to a specificed out.dir location
 * o Executes any executable yaml generated to out.dir location.
 * @author z091182
 *
 */
@Component
@Slf4j
class BlueprintTemplateInterpretorService implements  FindExecutableYamlNoRepoTrait, XLCliTrait {
	
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
	
	@Value('${repo.target.branch:refs/heads/master}')
	String repoTargetBranch

	@Value('${ado.workitemid:}')
	String adoWorkitemId

	def answers = [:]
	
	@Autowired
	GitService gitService
	
	@Autowired
	PolicyManagementService policyManagementService
	
	@Autowired
	CodeManagementService codeManagementService
	
	@Autowired
	ProjectManagementService projectManagementService
	
	@Autowired
	MemberManagementService memberManagementService

	
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
		//write answers file.
//		def answersOut = new YamlBuilder()
//		answersOut.call(answers)
//		String answersStr = answersOut.toString()
		File pipelineDir = new File(outDir, pipelineFolder)
		if (!pipelineDir.exists()) {
			pipelineDir.mkdirs()
		}
		loadXLCli(pipelineDir)
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
	
	def runPullRequestOnChanges() {
		String repoPath = repoDir.absolutePath
		String outDirPath = outDir.absolutePath
		String fPattern = outDirPath.substring(repoPath.length()+1)
		fPattern = "$fPattern/${pipelineFolder}"
		if (adoWorkitemId && adoWorkitemId.length() > 0) {
			gitService.pushChanges(repoDir, fPattern, "Adding pipline changes \n#${adoWorkitemId}")
		} else {
			gitService.pushChanges(repoDir, fPattern, "Adding pipline changes")
		}
		def projectData = projectManagementService.getProject('', adoProject)
		def repoData = codeManagementService.getRepo('', projectData, repoDir.name)
		
		def policies = policyManagementService.clearBranchPolicies('', projectData, repoData.id, repoTargetBranch)
		try {
			String branchName = gitService.getBranchName(repoDir)
			def pullRequestData = [sourceRefName: branchName, targetRefName: repoTargetBranch, title: "Update pipeline", description: "Making changes to pipeline implementation"]
			def prd = codeManagementService.createPullRequest('', projectData.id, repoData.id, pullRequestData)
			String prId = "${prd.pullRequestId}"
			def id = [id: prd.createdBy.id]
			def opts = [deleteSourceBranch: true, mergeCommitMessage: 'Update pipeline merge', mergeStrategy: 'rebase', autoCompleteIgnoreConfigIds: [], bypassPolicy: false, transitionWorkItems: false]
			def updateData = [completionOptions: opts, status: 'completed', lastMergeSourceCommit: prd.lastMergeSourceCommit]
			//codeManagementService.updatePullRequest('', projectData.id, repoData.id, prId, updateData)
			while (true) {
				try {
					System.sleep(5000)
				} catch (e) {}
				prd = codeManagementService.updatePullRequest('', projectData.id, repoData.id, prId, updateData)
				String status = "${prd.status}"
				if (status != 'active') break
			}
		} catch (Exception e) {
			log.error(e.message)
			throw e
		} 
		finally {
			policyManagementService.restoreBranchPolicies('', projectData, repoData.id, repoTargetBranch, policies)
		}
	}
	
	def getIdentity(String uniqueName) {
		def identities = memberManagementService.getIdentity('', uniqueName)
		if (identities.size() > 0) return identities[0]
		return null
	}
		
}
