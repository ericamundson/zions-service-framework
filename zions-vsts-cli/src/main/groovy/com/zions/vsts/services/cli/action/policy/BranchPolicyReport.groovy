package com.zions.vsts.services.cli.action.policy;

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.policy.PolicyManagementService
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import java.io.File


@Component
@Slf4j
class BranchPolicyReport implements CliAction {
	PolicyManagementService policyManagementService
	ProjectManagementService projectManagementService
	
	@Autowired
	public BranchPolicyReport(PolicyManagementService policyService, ProjectManagementService projectManagementService) {
		this.policyManagementService = policyService
		this.projectManagementService = projectManagementService
	}

	@Override
	public def execute(ApplicationArguments data) {
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		String projectName = ""
		if (data.getOptionValues('tfs.project') != null) {
			projectName = data.getOptionValues('tfs.project')[0]
		}
		// create file
		def reportFile = new File("PolicyReport.txt")
		reportFile.delete()
		reportFile.createNewFile()
		reportFile.text = "Branch Policy Report \n" +"Date: "+new Date().format('MM-dd-yyyy')+" \n \n"
		// check for single project
		if (projectName != null && projectName != "") {
			def projectData = getProjectData(collection, projectName)
			if (projectData != null) {
				System.out.println("Getting branch policy report for project ${projectData.name}")
				def result = policyManagementService.getBranchPolicyReport(collection, projectData)
				// TODO: output report ??
				printReport("${projectData.name}", result, reportFile)
			}
		} else {
			System.out.println("Getting projects ...")
			def projects = projectManagementService.getProjects(collection)
			projects.value.each { projectData ->
				//def projectData = getProjectData(collection, "${project.name}")
				//if (projectData != null) {
					System.out.println("Getting branch policy report for project ${projectData.name}")
					def result = policyManagementService.getBranchPolicyReport(collection, projectData)
					printReport("${projectData.name}", result, reportFile)
				//}
			}
		}
		return null
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.token']
		required.each { name ->
			if (!args.containsOption(name)) {
				log.debug("Missing required argument:  ${name}.  Exiting ...")
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	
	private def getProjectData(def collection, def project) {
		def projectData = projectManagementService.getProject(collection, project)
		if (projectData == null) {
			log.debug("Exception occurred trying to get project data for project "+project+".  Returning null ...")
			System.out.println("Exception occurred trying to get project data for project "+project+".  Returning null ...")
			return null
		}
		return projectData
	}
	
	private def printReport(def projectName, def policyReport, def rptFile) {
		System.out.println("Project: ${projectName}")
		log.debug("Project: ${projectName}")
		rptFile.text += "Project: ${projectName} \n"
		policyReport.repos.each { repo ->
			System.out.println("  Repository: ${repo.repoName}:")
			log.debug("  Repository: ${repo.repoName}:")
			rptFile.text += "  Repository: ${repo.repoName}: \n"
			repo.branches.each { branch ->
				System.out.println("    Branch: ${branch.branchName}:")
				rptFile.text += "    Branch: ${branch.branchName}: \n"
				def policyInfo = branch.policyInfo
				System.out.println("      Has Build Policy: ${policyInfo.hasBuildPolicy}")
				rptFile.text += "      Has Build Policy: ${policyInfo.hasBuildPolicy} \n"
				if (policyInfo.hasBuildPolicy) {
					System.out.println("        Validation Build : ${policyInfo.ciBuildName}")
					rptFile.text += "        Validation Build : ${policyInfo.ciBuildName} \n"
				}
				System.out.println("      Has Minimum Reviewers Policy: ${policyInfo.hasMinimumReviewersPolicy}")
				rptFile.text += "      Has Minimum Reviewers Policy: ${policyInfo.hasMinimumReviewersPolicy} \n"
				if (policyInfo.hasMinimumReviewersPolicy) {
					System.out.println("        Minimum # of Reviewers: ${policyInfo.minimumNumReviewers}")
					System.out.println("        Can Approve Own Changes: ${policyInfo.creatorCanApprove}")
					System.out.println("        Reset Approvals On Change: ${policyInfo.resetIfChanged}")
					rptFile.text += "        Minimum # of Reviewers: ${policyInfo.minimumNumReviewers} \n"
					rptFile.text += "        Can Approve Own Changes: ${policyInfo.creatorCanApprove} \n"
					rptFile.text += "        Reset Approvals On Change: ${policyInfo.resetIfChanged} \n"
					
				}
				System.out.println("      Has Merge Strategy Policy: ${policyInfo.hasMergeStrategyPolicy}")
				System.out.println("      Has Linked Work Items Policy: ${policyInfo.hasLinkedWorkItemsPolicy}")
				System.out.println("      Has Comment Resolution Policy: ${policyInfo.hasCommentResolutionPolicy}")
				rptFile.text += "      Has Merge Strategy Policy: ${policyInfo.hasMergeStrategyPolicy} \n"
				rptFile.text += "      Has Linked Work Items Policy: ${policyInfo.hasLinkedWorkItemsPolicy} \n"
				rptFile.text += "      Has Comment Resolution Policy: ${policyInfo.hasCommentResolutionPolicy} \n \n"
			}
		}
		rptFile.text += " \n"
		return null
	}
	
}