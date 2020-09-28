package com.zions.pipeline.services.yaml.template.execution
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.zions.pipeline.services.git.GitService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.policy.PolicyManagementService
import com.zions.pipeline.services.mixins.CliRunnerTrait

import groovy.yaml.YamlBuilder

import groovy.util.logging.Slf4j

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
class RunXLBlueprints implements IExecutableYamlHandler, CliRunnerTrait {


	@Value('${xl.user:}')
	String xlUser

	@Value('${xl.password:}')
	String xlPassword

	@Value('${pipeline.folder:.pipeline}')
	String pipelineFolder

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

	def handleYaml(def yaml, File repo, def locations, String branch, String project) {
		boolean useProxy = false
		if (yaml.useProxy) {
			useProxy = yaml.useProxy
		}
		def repoName = yaml.repoName
		//def project = yaml.project

		def projectData = projectManagementService.getProject('', project)
		def repoData = codeManagementService.getRepo('', projectData, repoName)

		def outrepo = gitService.loadChanges(repoData.remoteUrl, repoName)

		File loadDir = new File(outrepo, "${pipelineFolder}")
		if (!loadDir.exists()) {
			loadDir.mkdirs()
		}
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

			def bpOutrepo = gitService.loadChanges(bpRepoData.remoteUrl, bpRepoName)

			YamlBuilder yb = new YamlBuilder()

			yb( bp.answers )

			String answers = yb.toString()

			File aF = new File("${outrepo.absolutePath}/${pipelineFolder}/${blueprint}-answers.yaml")
			def sAF = aF.newDataOutputStream()
			sAF << answers
			sAF.close()
			def env = null
			if (useProxy) {
				env = [key:"https_proxy", value:"https://${xlUser}:${xlPassword}@172.18.4.115:8080"]
			}
			def arg = [line: "${option} ${outrepo.absolutePath}/${pipelineFolder}/xl blueprint -a \"${outrepo.absolutePath}/${pipelineFolder}/${blueprint}-answers.yaml\" -l ${bpOutrepo.absolutePath} -b \"${blueprint}\" -s"]
			run(command, "${outrepo.absolutePath}/${pipelineFolder}", arg, env, log)
		}
		def policies = policyManagementService.clearBranchPolicies('', projectData, repoData.id, 'refs/heads/master')
		gitService.pushChanges(outrepo)
		policyManagementService.restoreBranchPolicies('', projectData, repoData.id, 'refs/heads/master', policies)
	}

	boolean performExecute(def yaml, List<String> locations) {
		if (yaml.dependencies) return false
		for (String dep in yaml.dependencies) {
			if (locations.contains(dep)) return true
		}
		return false
	}

	def loadXLCli(File loadDir) {
		String osname = System.getProperty('os.name')

		if (osname.contains('Windows')) {
			InputStream istream = this.getClass().getResourceAsStream('/xl/windows/xl.exe')
			File of = new File(loadDir, 'xl.exe')
			def aos = of.newDataOutputStream()
			aos << istream
			aos.close()
		} else {
			InputStream istream = this.getClass().getResourceAsStream('/xl/linux/xl')
			File of = new File(loadDir, 'xl')
			def aos = of.newDataOutputStream()
			aos << istream
			aos.close()

		}
	}
}
