package com.zions.vsts.services.action.policy;

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
		def reportFile = new File("PolicyReport.html")
		reportFile.delete()
		reportFile.createNewFile()
		def currentDate = new Date().format('MM-dd-yyyy')
		reportFile.text = '''
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>Branch Policy Report</title>
    <style type="text/css">
 body { font: 12pt Georgia, "Times New Roman", Times, serif; line-height: 1.3; padding-top: 10px; } div.header { display: block; text-align: center; position: running(header); width: 100%; } div.footer { display: block; text-align: center; position: running(footer); width: 100%; } @page { /* switch to landscape */ size: landscape; /* set page margins */ margin: 0.5cm; @top-center { content: element(header); } @bottom-center { content: element(footer); } @bottom-right { content: counter(page) " of " counter(pages); } } .custom-page-start { margin-top: 10px; } .trueInd { color: green; } .falseInd { color: green; }
  </style>
</head>
<body>
<h1>Branch Policy Report &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;	Date: '''

		reportFile.text += "&nbsp; ${currentDate}</h1>"

		// check for single project
		if (projectName != null && projectName != "" && projectName.toLowerCase() != "all") {
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
				System.out.println("Getting branch policy report for project ${projectData.name}")
				def result = policyManagementService.getBranchPolicyReport(collection, projectData)
				printReport("${projectData.name}", result, reportFile)
			}
		}
		reportFile.text += '''
</div>
</body>
</html>
'''
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
		System.out.println("Writing branch policy report ...")
		//System.out.println("Project: ${projectName}")
		//log.debug("Project: ${projectName}")

		rptFile.text += '''
<div class="custom-page-start" style="page-break-before: always;">
    <h2>Project: '''
		rptFile.text += "&nbsp; ${projectName}</h2>"
		policyReport.repos.each { repo ->
			//System.out.println("  Repository: ${repo.repoName}")
			//log.debug("  Repository: ${repo.repoName}:")
			rptFile.text += "<p><b>Repository:</b> <span style=\"background-color:lightblue;\">${repo.repoName}</span></p>"
			if (repo.branches.isEmpty()) {
				rptFile.text += "<p>   &nbsp;&nbsp;<span style=\"background-color:red;\">Repository has no branches or unable to access due to permissions.</span></p>"
			} else {
				repo.branches.each { branch ->
					//System.out.println("    Branch: ${branch.branchName}:")
					def orgName = "ZionsETO"
					if ("${repo.repoURL}".contains("/eto-dev/")) {
						orgName = "eto-dev"
					}
					rptFile.text += "<p>   &nbsp;&nbsp;<b>Branch:</b> <a href=\"https://dev.azure.com/${orgName}/${projectName}/_settings/repositories?_a=policiesMid&repo=${repo.repoId}&refs=refs/heads/${branch.branchName}\"> ${branch.branchName}</a><br /><ul>"
					//rptFile.text += "   <div class=\"branchPolicies\">"
					def policyInfo = branch.policyInfo
					//System.out.println("      Has Build Policy: ${policyInfo.hasBuildPolicy}")
					rptFile.text += "<li><b>Has Build Policy:</b>"
					if (policyInfo.hasBuildPolicy) {
						rptFile.text += "<span style=\"color:green;\"> Yes</span></li>"
					} else {
						rptFile.text += "<span style=\"color:red;\"> No</span></li>"
					}

					if (policyInfo.hasBuildPolicy) {
						//System.out.println("        Validation Build : ${policyInfo.ciBuildName}")
						rptFile.text += "<ul><li><b>Validation Build:</b> ${policyInfo.ciBuildName}</ul></li>"
					}
					//System.out.println("      Has Minimum Reviewers Policy: ${policyInfo.hasMinimumReviewersPolicy}")
					rptFile.text += "<li><b>Has Minimum Reviewers Policy:</b>"
					if (policyInfo.hasMinimumReviewersPolicy) {
						rptFile.text += "<span style=\"color:green;\"> Yes</span></li><ul>"
						//System.out.println("        Minimum # of Reviewers: ${policyInfo.minimumNumReviewers}")
						//System.out.println("        Can Approve Own Changes: ${policyInfo.creatorCanApprove}")
						//System.out.println("        Reset Approvals On Change: ${policyInfo.resetIfChanged}")
						rptFile.text += "<li><b>Minimum # of Reviewers:</b> ${policyInfo.minimumNumReviewers}</li>"
						rptFile.text += "<li><b>Can Approve Own Changes:</b> ${policyInfo.creatorCanApprove}</li>"
						rptFile.text += "<li><b>Reset Approvals On Change:</b> ${policyInfo.resetIfChanged}</li></ul>"
					} else {
						rptFile.text += "<span style=\"color:red;\"> No</span></li>"
					}
					//System.out.println("      Has Merge Strategy Policy: ${policyInfo.hasMergeStrategyPolicy}")
					//System.out.println("      Has Linked Work Items Policy: ${policyInfo.hasLinkedWorkItemsPolicy}")
					//System.out.println("      Has Comment Resolution Policy: ${policyInfo.hasCommentResolutionPolicy}")
					rptFile.text += "<li><b>Has Merge Strategy Policy:</b>"
					if (policyInfo.hasMergeStrategyPolicy) {
						rptFile.text += "<span style=\"color:green;\"> Yes</span><br />"
						rptFile.text += "&nbsp;&nbsp;&nbsp; <b>Types of merges allowed:</b><ul>"
						// check for and print allowed merge types
						if (policyInfo.allowNoFastForward) {
							rptFile.text += "<li> Basic merge (no fast-forward)</li>"
						}
						if (policyInfo.allowSquash) {
							rptFile.text += "<li> Squash merge</li>"
						}
						if (policyInfo.allowRebase) {
							rptFile.text += "<li> Rebase and fast-forward</li>"
						}
						if (policyInfo.allowRebaseMerge) {
							rptFile.text += "<li> Rebase with merge commit</li>"
						}
						rptFile.text += "</ul></li>"
					} else {
						rptFile.text += "<span style=\"color:red;\"> No</span></li>"
					}
					rptFile.text += "<li><b>Has Linked Work Items Policy:</b>"
					if (policyInfo.hasLinkedWorkItemsPolicy) {
						rptFile.text += "<span style=\"color:green;\"> Yes</span></li>"
					} else {
						rptFile.text += "<span style=\"color:red;\"> No</span></li>"
					}
					rptFile.text += "<li><b>Has Comment Resolution Policy:</b>"
					if (policyInfo.hasCommentResolutionPolicy) {
						rptFile.text += "<span style=\"color:green;\"> Yes</span></li>"
					} else {
						rptFile.text += "<span style=\"color:red;\"> No</span></li>"
					}
					rptFile.text += "</ul></p>"
				}
			}
		}
		rptFile.text += '''
</div>

'''
		return null
	}
	
}