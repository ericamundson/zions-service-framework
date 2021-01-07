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
import com.zions.pipeline.services.mixins.CliRunnerTrait

import groovy.util.logging.Slf4j

import com.zions.pipeline.services.yaml.template.execution.IExecutableYamlHandler
import com.zions.pipeline.services.git.GitService
import com.zions.vsts.services.policy.PolicyManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.admin.member.MemberManagementService
import org.eclipse.jgit.api.Git


/**
 * Performs the following behaviors:
 * o Runs a XL Blueprint to a specificed out.dir location
 * o Executes any executable yaml generated to out.dir location.
 * @author z091182
 *
 */
@Component
@Slf4j
class RemoteBlueprintTemplateInterpretorService implements  FindExecutableYamlNoRepoTrait, XLCliTrait, CliRunnerTrait {

	@Autowired
	Map<String, IExecutableYamlHandler> yamlHandlerMap;

	@Value('${blueprint.repo.url:}')
	String blueprintRepoUrl

	@Value('${blueprint:}')
	String blueprint

	@Value('${blueprint.folder.name:}')
	String blueprintFolderName

	@Value('${out.repo.url:}')
	String outRepoUrl

	@Value('${out.dir.name:}')
	String outDirName

	@Value('${pipeline.folder:.pipeline}')
	String pipelineFolder

	@Value('${in.placeholder.delimiters:[[,]]}')
	String[] inDelimiters

	@Value('${ado.project:DTS}')
	String adoProject

	@Value('${repo.target.branch:}')
	String repoTargetBranch

	@Value('${ado.workitemid:}')
	String adoWorkitemId

	@Value('${input.answers:}')
	String inputAnswers


	File repoDir
	File outDir

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

	@Value('${blueprint.config.context:Test}')
	String configContext


	def outputPipeline() {

		File blueprintDir = gitService.loadChanges(blueprintRepoUrl)
		//		if (blueprintFolderName && blueprintFolderName.length() > 0) {
		//			blueprintDir = new File(blueprintDir, blueprintFolderName)

		//		}
		Git git = null
		try {

			git = gitService.open(blueprintDir)
			fixBlueprint(blueprintDir, "${blueprintFolderName}/${blueprint}")
			if (repoTargetBranch == null || repoTargetBranch.length() == 0) {
				repoTargetBranch = null
			}
			repoDir = gitService.loadChanges(outRepoUrl, null, repoTargetBranch)
			gitService.checkout(repoDir, "blueprint/${new Date().time}", true)
			updateIgnore(repoDir)


			outDir = repoDir
			if (outDirName && outDirName.length() > 0) {
				outDir = new File(outDir, outDirName)
			}
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
			buildConfigYaml(pipelineDir, blueprintDir)

			File startupBat = new File(pipelineDir, 'startup.bat')
			def os = startupBat.newDataOutputStream()
			os << 'start /W cmd /C %*'
			os.close()
			String oss = System.getProperty('os.name')
			String command = 'cmd'
			String option = '/c'
			String sep = '\r\n'
			if (!oss.contains('Windows')) {
				command = '/bin/sh'
				option = '-c'
				sep = '\n'
			}

			//Generate pipeline
			if (inputAnswers && inputAnswers.size() > 0) {
				String inputAns = inputAnswers.replace('&&',"${sep}")
				inputAns = "---${sep}" + inputAns
				//			YamlBuilder yb = new YamlBuilder()
				//
				//			yb( inputAns )
				//
				//			String answers = yb.toString()

				File aF = new File(outDir, "${pipelineFolder}/answers.yaml")
				def sAF = aF.newDataOutputStream()
				sAF << inputAns
				sAF.close()
				def args = [line: "${option} xl blueprint --config \"./.xebialabs/config.yaml\" -a ./answers.yaml -b \"${blueprintFolderName}/${blueprint}\" -s"]
				if (oss.contains('Windows') && configContext == 'Dev') {
					args.line = args.line.replace('/','\\')
					args.line = args.line.replace('\\c', '/c')
				}
				run(command, "${outDir}/${pipelineFolder}", args, null, log)
			} else {
				run("${outDir}/${pipelineFolder}/startup.bat", "${outDir}/${pipelineFolder}", [line: "${outDir}/${pipelineFolder}/xl blueprint  -l ${blueprintDir} -b \"${blueprint}\" "], null, log)

			}
		} catch (e) {
			throw e
		} finally {
			if (git) {
				gitService.close(git)
			}
		}

		//fix placeholders.
		//		new AntBuilder().replace(dir: "${outDir}/${pipelineFolder}") {
		//			replacefilter( token: "${inDelimiters[0]}", value: '{{')
		//			replacefilter( token: "${inDelimiters[1]}", value: '}}')
		//		}

	}

	def runExecutableYaml() {
		String branchName = gitService.getBranchName(repoDir)
		def exeYaml = findExecutableYaml()
		for (def yaml in exeYaml) {
			for (def exe in yaml.executables) {

				IExecutableYamlHandler yamlHandler = yamlHandlerMap[exe.type]
				if (yamlHandler) {
					try {
						yamlHandler.handleYaml(exe, repoDir, [], branchName, adoProject)
					} catch (e) {
						log.error("Failed running executable yaml:  ${exe.type} :: ${e.message}")
						e.printStackTrace()
					}
				}
			}
		}
	}

	String getProjectName(String url) {
		String[] uSplit = url.split('/')
		String outStr = null
		try {
			outStr = uSplit[4]
		} catch (e) {}
		return outStr
	}


	def runPullRequestOnChanges() {
		String repoPath = repoDir.absolutePath
		String outDirPath = outDir.absolutePath
		String fPattern = "${pipelineFolder}"
		if (repoPath != outDirPath) {
			fPattern = outDirPath.substring(repoPath.length()+1)
			fPattern = "$fPattern/${pipelineFolder}"
			fPattern = fPattern.replace('\\', '/')
		}
		String projectName = getProjectName(outRepoUrl)
		log.info("fPattern:  ${fPattern}")
		def projectData = projectManagementService.getProject('', projectName)
		int nIndex = outRepoUrl.lastIndexOf('/')+1
		String repoName = outRepoUrl.substring(nIndex)
		def repoData = codeManagementService.getRepo('', projectData, repoName)
		if (adoWorkitemId && adoWorkitemId.length() > 0) {
			gitService.pushChanges(repoDir, fPattern, "Adding pipline changes \n#${adoWorkitemId}")
		} else {
			gitService.pushChanges(repoDir, fPattern, "Adding pipeline changes")
		}

		def policies = policyManagementService.clearBranchPolicies('', projectData, repoData.id, repoTargetBranch)
		try {
			String branchName = gitService.getBranchName(repoDir)
			gitService.checkout(repoDir, repoTargetBranch)
			gitService.deleteBranch(repoDir, branchName)
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
