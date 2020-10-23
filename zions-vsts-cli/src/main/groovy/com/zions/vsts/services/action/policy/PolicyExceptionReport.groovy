package com.zions.vsts.services.action.policy;

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.policy.PolicyManagementService
import groovy.util.logging.Slf4j
import java.io.File


@Component
@Slf4j
class PolicyExceptionReport implements CliAction {
	PolicyManagementService policyManagementService
	ProjectManagementService projectManagementService
	
	@Autowired
	public PolicyExceptionReport(PolicyManagementService policyService, ProjectManagementService projectManagementService) {
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
		def reportFile = new File("PolicyExceptionReport.html")
		reportFile.delete()
		reportFile.createNewFile()
		def currentDate = new Date().format('MM-dd-yyyy')
		reportFile.text = '''
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>Policy Exception Report</title>
    <style type="text/css">
 body { font: 12pt Georgia, "Times New Roman", Times, serif; line-height: 1.3; padding-top: 10px; } div.header { display: block; text-align: center; position: running(header); width: 100%; } div.footer { display: block; text-align: center; position: running(footer); width: 100%; } @page { /* switch to landscape */ size: landscape; /* set page margins */ margin: 0.5cm; @top-center { content: element(header); } @bottom-center { content: element(footer); } @bottom-right { content: counter(page) " of " counter(pages); } } .custom-page-start { margin-top: 10px; } .trueInd { color: green; } .falseInd { color: green; }
  </style>
</head>
<body>
<h1>Policy Exception Report &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;	Date: '''

		reportFile.text += "&nbsp; ${currentDate}</h1>"

		// check for single project
		if (projectName != null && projectName != "" && projectName.toLowerCase() == "all") {
			System.out.println("Getting projects ...")
			def projects = projectManagementService.getProjects(collection)
			projects.value.each { projectData ->
				System.out.println("Getting policy exception report for project ${projectData.name}")
				def result = policyManagementService.getPolicyExceptionReport(collection, projectData)
				printReport("${projectData.name}", result, reportFile)
			}
		} else {
			String[] projectNames = projectName.split(',')
			projectNames.each { String pName ->
				def projectData = getProjectData(collection, pName)
				if (projectData != null) {
					System.out.println("Getting policy exception report for project ${projectData.name}")
					def result = policyManagementService.getPolicyExceptionReport(collection, projectData)
					printReport("${projectData.name}", result, reportFile)
				}
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
		System.out.println("Writing policy exception report ...")
		//System.out.println("Project: ${projectName}")
		//log.debug("Project: ${projectName}")

		rptFile.text += '''
<div class="custom-page-start" style="page-break-before: always;">
    <h2>Project: '''
		rptFile.text += "&nbsp; ${projectName}</h2>"
		policyReport.repos.each { repo ->
			//System.out.println("  Repository: ${repo.repoName}")
			//log.debug("  Repository: ${repo.repoName}:")
			def firstBranch = true
			if (repo.branches.isEmpty()) {
				rptFile.text += "<p><b>Repository:</b> <span style=\"background-color:lightblue;\">${repo.repoName}</span></p>"
				rptFile.text += "<p>   &nbsp;&nbsp;<span style=\"background-color:red;\">Repository has no branches or unable to access due to permissions.</span></p>"
			} else {
				repo.branches.each {	 branch ->
					//System.out.println("    Branch: ${branch.branchName}:")
					def orgName = "ZionsETO"
					if ("${repo.repoURL}".contains("/eto-dev/")) {
						orgName = "eto-dev"
					}
					def policyInfo = branch.policyInfo
					if (hasPolicyException(policyInfo)) {
						if (firstBranch) {
							rptFile.text += "<p><b>Repository:</b> <span style=\"background-color:lightblue;\">${repo.repoName}</span></p>"
							firstBranch = false
						}
						rptFile.text += "<p>   &nbsp;&nbsp;<b>Branch:</b> <a href=\"https://dev.azure.com/${orgName}/${projectName}/_settings/repositories?_a=policiesMid&repo=${repo.repoId}&refs=refs/heads/${branch.branchName}\"> ${branch.branchName}</a><br /><ul>"
						//rptFile.text += "   <div class=\"branchPolicies\">"
						if (!policyInfo.hasBuildPolicy) {
							rptFile.text += "<li><span style=\"color:red;\">Build Policy not enforced</span></li>"
						}
						if (!policyInfo.hasMinimumReviewersPolicy) {
							rptFile.text += "<li><span style=\"color:red;\">Minimum Reviewers Policy not enforced</span></li>"
						} else {
							if ("${policyInfo.minimumNumReviewers}".toInteger() < 1) {
								rptFile.text += "<li><span style=\"color:red;\">Minimum Reviewers Policy has invalid # of reviewers. Minimum should be 1</span>"
							}
							if (policyInfo.creatorCanApprove) {
								rptFile.text += "<li><span style=\"color:red;\">Creator should not be able to approve their own Pull Request</span>"
							}
							if (!policyInfo.resetIfChanged) {
								rptFile.text += "<li><span style=\"color:red;\">Reset Approvals On Change should be set to True</span>"
							}
						}
						if (!policyInfo.hasMergeStrategyPolicy) {
							rptFile.text += "<li><span style=\"color:red;\">Merge Strategy Policy not enforced</span></li>"
						} else {
							// check for and allowed merge types
							if (!policyInfo.allowNoFastForward) {
								rptFile.text += "<li><span style=\"color:red;\"> Basic (no fast-forward) merge strategy should be allowed</span></li>"
							}
							if (policyInfo.allowRebase) {
								rptFile.text += "<li><span style=\"color:red;\"> Rebase and Fast-forward merge strategy is allowed.</span></li>"
							}
							if (policyInfo.allowRebaseMerge) {
								rptFile.text += "<li><span style=\"color:red;\"> Rebase with Merge Commit merge strategy is allowed.</span></li>"
							}
						}
						if (!policyInfo.hasLinkedWorkItemsPolicy) {
							rptFile.text += "<li><span style=\"color:red;\">Linked Work Items Policy not enforced</span></li>"
						}
						if (!policyInfo.hasCommentResolutionPolicy) {
							rptFile.text += "<li><span style=\"color:red;\">Comment Resolution Policy not enforced</span></li>"
						}
						rptFile.text += "</ul></p>"
					}
				}
			}
		}
		rptFile.text += '''
</div>

'''
		return null
	}

	private boolean hasPolicyException(def policyInfo) { 
		if (!policyInfo.hasBuildPolicy ||
			!policyInfo.hasLinkedWorkItemsPolicy ||
			!policyInfo.hasCommentResolutionPolicy ||
			hasMinimumReviewersPolicyException(policyInfo) ||
			hasMergeStrategyPolicyException(policyInfo)) {
			return true;
		}
	}
		
	private boolean hasMinimumReviewersPolicyException(def policyInfo) {
		// check the policy config for exceptions
		if (!policyInfo.hasMinimumReviewersPolicy ||
			(policyInfo.hasMinimumReviewersPolicy && (policyInfo.minimumNumReviewers < 1 || policyInfo.creatorCanApprove || !policyInfo.resetIfChanged))) {
			return true
		}
		return false;
	}

	private boolean hasMergeStrategyPolicyException(def policyInfo) {
		// check for allowed merge types
		if (!policyInfo.hasMergeStrategyPolicy ||
			(policyInfo.hasMergeStrategyPolicy && (!policyInfo.allowNoFastForward || policyInfo.allowRebase || policyInfo.allowRebaseMerge))) {
			return true
		}
		return false;

	}

}

enum PolicyType {
	BUILD, REVIEWERS, MERGE, WORKITEM, COMMENTRES
}