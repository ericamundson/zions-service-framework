package com.zions.pipeline.services.yaml.template.execution
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.zions.pipeline.services.git.GitService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.policy.PolicyManagementService
import com.zions.pipeline.services.mixins.CliRunnerTrait
import com.zions.pipeline.services.mixins.FeedbackTrait
import com.zions.pipeline.services.mixins.XLCliTrait

import groovy.yaml.YamlBuilder

import groovy.util.logging.Slf4j
import org.eclipse.jgit.api.Git


/**
 * Accepts yaml in the form:
 * <pre>
 * executables:
 * - name: reponame
 *   type: runXLBlueprints
 *   project: AgriculturalFinance
 *   blueprints:
 *   - name: windows-app
 *     repoName: bpRepo
 *     project: DTS
 *     answers:
 *       ans1: stuff
 *       ans2: stuff  
 * </pre>
 * @author z091182
 *
 */
@Component
@Slf4j
class RunXLBlueprints implements IExecutableYamlHandler, CliRunnerTrait, XLCliTrait, FeedbackTrait {


	@Value('${xl.user:}')
	String xlUser

	@Value('${xl.password:}')
	String xlPassword

	@Value('${pipeline.folder:.pipeline}')
	String pipelineFolder

	@Value('${blueprint.config.context:Test}')
	String configContext

	@Autowired
	GitService gitService
	@Autowired
	CodeManagementService codeManagementService
	@Autowired
	ProjectManagementService projectManagementService
	@Autowired
	PolicyManagementService policyManagementService

	public RunXLBlueprints() {
	}

	def handleYaml(def yaml, File repo, def locations, String branch, String project, String pipelineId = null, String userName = null) {
		if (yaml.project) {
			project = yaml.project
		}
		boolean useProxy = false
		if (yaml.useProxy) {
			useProxy = yaml.useProxy
		}
		def repoName = yaml.repoName
		//def project = yaml.project
		logInfo(pipelineId, "Running blueprints against repo: ${repoName}")
		def projectData = projectManagementService.getProject('', project)
		def repoData = codeManagementService.getRepo('', projectData, repoName)

		def outrepo = gitService.loadChanges(repoData.remoteUrl, repoName, null, false, userName)
		gitService.checkout(outrepo, "blueprint/${new Date().time}", true)
		

		File loadDir = new File(outrepo, "${pipelineFolder}")
		if (yaml.outDir) {
			String outDir = "${yaml.outDir}"
			loadDir = new File(outrepo, "${outDir}/${pipelineFolder}")
		}
		if (!loadDir.exists()) {
			loadDir.mkdirs()
		}
		updateIgnore(loadDir)
		loadXLCli(loadDir)
		String os = System.getProperty('os.name')
		String command = 'cmd'
		String option = '/c'
		if (!os.contains('Windows')) {
			command = '/bin/sh'
			option = '-c'
		}
		for (def bp in yaml.blueprints) {
			String bpProjectName = bp.project
			String bpRepoName = bp.repoName
			String blueprint = bp.name
			def bpProjectData = projectManagementService.getProject('', bpProjectName)
			def bpRepoData = codeManagementService.getRepo('', bpProjectData, bpRepoName)

			Git git = null
			try {
				def bpOutrepo = null
				if (configContext == 'Dev') {
					bpOutrepo = gitService.loadChanges(bpRepoData.remoteUrl, bpRepoName)
					git = gitService.open(bpOutrepo)
					fixBlueprint(bpOutrepo, "${bp.blueprintFolder}/${blueprint}")
				}
				
				buildConfigYaml(loadDir, bpOutrepo)
				File bpFolder = bpOutrepo
//				if (bp.blueprintFolder) {
//					String bpFolderStr = bp.blueprintFolder
//					bpFolder = new File(bpOutrepo, bpFolderStr)
//				}

				YamlBuilder yb = new YamlBuilder()

				yb( bp.answers )

				String answers = yb.toString()

				File aF = new File("${loadDir.absolutePath}/${blueprint}-answers.yaml")
				def sAF = aF.newDataOutputStream()
				sAF << answers
				sAF.close()
				def env = null
				if (useProxy) {
					env = [key:"https_proxy", value:"https://${xlUser}:${xlPassword}@172.18.4.115:8080"]
				}
				def arg = [line: "${option} ${loadDir.absolutePath}/xl blueprint --config \"${loadDir.absolutePath}/.xebialabs/config.yaml\" -a \"${loadDir.absolutePath}/${blueprint}-answers.yaml\" -b \"${bp.blueprintFolder}/${blueprint}\" -s"]
				if (os.contains('Windows') && configContext == 'Dev') {
					arg.line = arg.line.replace('/','\\')
					arg.line = arg.line.replace('\\c', '/c')
				}
				run(command, "${loadDir.absolutePath}", arg, env, log, pipelineId)
			} catch (e) {
				logError(pipelineId, e.message)
				throw e
			} finally {
				if (git) {
					gitService.close(git)
				}
			}
		}
		//		def policies = policyManagementService.clearBranchPolicies('', projectData, repoData.id, 'refs/heads/master')
		//		gitService.pushChanges(outrepo)
		//		policyManagementService.restoreBranchPolicies('', projectData, repoData.id, 'refs/heads/master', policies)
		runPullRequestOnChanges(outrepo, loadDir, project, projectData, repoData, pipelineId, userName)
	}

	def runPullRequestOnChanges(File repoDir, File outDir, String adoProject, def projectData, def repoData, String pipelineId, String userName) {
		String repoPath = repoDir.absolutePath
		String outDirPath = outDir.absolutePath
		String fPattern = "${pipelineFolder}"
		if (repoPath != outDirPath) {
			fPattern = outDirPath.substring(repoPath.length()+1)
			//fPattern = "$fPattern/${pipelineFolder}"
		}
		//fPattern = "$fPattern/${pipelineFolder}"
		String repoTargetBranch = repoData.defaultBranch
		gitService.pushChanges(repoDir, fPattern, "Adding pipeline changes")


		def policies = policyManagementService.clearBranchPolicies('', projectData, repoData.id, repoTargetBranch)
		try {
			String branchName = gitService.getBranchName(repoDir)
			gitService.checkout(repoDir, repoTargetBranch)
			gitService.deleteBranch(repoDir, branchName)
			def pullRequestData = [sourceRefName: branchName, targetRefName: repoTargetBranch, title: "Update pipeline", description: "Making changes to pipeline implementation"]
			def prd = codeManagementService.createPullRequest('', projectData.id, repoData.id, pullRequestData)
			String prId = "${prd.pullRequestId}"
			def id = [id: prd.createdBy.id]
			String pullRequestComment = "Update pipeline merge"
			if (userName && userName.length() > 0) {
				pullRequestComment = "${pullRequestComment}, userName: ${userName}"
			}
			if (pipelineId && pipelineId.length() > 0){
				pullRequestComment = "${pullRequestComment}, pipelineId: ${pipelineId}"
			}
			def opts = [deleteSourceBranch: true, mergeCommitMessage: pullRequestComment, mergeStrategy: 'noFastForward', autoCompleteIgnoreConfigIds: [], bypassPolicy: false, transitionWorkItems: false]
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


	boolean performExecute(def yaml, List<String> locations) {
		if (yaml.dependencies) return false
		for (String dep in yaml.dependencies) {
			if (locations.contains(dep)) return true
		}
		return false
	}

}
