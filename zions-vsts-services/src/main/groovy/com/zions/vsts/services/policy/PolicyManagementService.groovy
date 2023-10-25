package com.zions.vsts.services.policy;

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import groovy.util.logging.Slf4j
import groovy.json.JsonBuilder;
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType
import groovy.yaml.YamlSlurper

import com.zions.common.services.notification.NotificationService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.build.BuildManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.release.ReleaseManagementService
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient

/**
 * 
 * @author James McNabb
 *
 */
@Component
@Slf4j
public class PolicyManagementService {

	@Autowired
	@Value('${tfs.build.properties.file:}')
	private String buildPropsFileName

	@Autowired
	private IGenericRestClient genericRestClient;

	@Autowired
	BuildManagementService buildManagementService
	
	@Autowired
	private CodeManagementService codeManagementService

	@Autowired
	ReleaseManagementService releaseManagementService
	
	@Autowired
	MemberManagementService memberManagementService

	@Autowired
	NotificationService notificationService
	
	private java.util.Properties branchProps

	private static final String ENFORCE_BUILD_VALIDATION = "enforce-build-validation"
	private static final String ENFORCE_MIN_APPROVERS = "enforce-minimum-approvers"
	private static final String ENFORCE_LINKED_WI = "enforce-linked-workitems"
	private static final String ENFORCE_COMMENT_RES = "enforce-comment-resolution"
	private static final String ENFORCE_MERGE_STRATEGY = "enforce-merge-strategy"
	private static final String NUM_MIN_APPROVERS = "num-min-reviewers"
	private static final int DEFAULT_NUM_APPROVERS = 1
	private static final String CI_BUILD_TEMPLATE = "build-template-ci"
	private static final String RELEASE_BUILD_TEMPLATE = "build-template-release"
	private static final String CI_BUILD_YAML_FILE = "build-yaml-file"
	private static final String CHECKMARX_STATUS_NAME = "checkmarx"
	private static final String SAST_STATUS_GENRE = "sast"
	private static final String SNOWCI_STATUS_NAME = "snowci-check"
	private static final String WATCHDOG_STATUS_NAME = "unauthorized-changes"
	private static final String CI_STATUS_GENRE = "continuous-integration"
	// ADO policy types
	private static final String BUILD_VALIDATION_POLICY_TYPE = "0609b952-1397-4640-95ec-e00a01b2c241"
	private static final String MIN_APPROVERS_POLICY_TYPE = "fa4e907d-c16b-4a4c-9dfa-4906e5d171dd"
	private static final String LINKED_WI_POLICY_TYPE = "40e92b44-2fe1-4dd6-b3d8-74a9c21d0c6e"
	private static final String COMMENT_RES_POLICY_TYPE = "c6a1889d-b943-4856-b76f-9e46bb6b0df2"
	private static final String MERGE_STRATEGY_POLICY_TYPE = "fa4e907d-c16b-4a4c-9dfa-4916e5d171ab"
	private static final String AUTOMATICALLY_INCLUDED_REVIEWERS_POLICY_TYPE = "fd2167ab-b0be-447a-8ec8-39368250530e"
	private static final String CUSTOM_STATUS_POLICY_TYPE = "cbdc66da-9728-4af8-aada-9a5a32e4a226"
	
	public PolicyManagementService() {
	}

	/**
	 *  This method handles ensuring that all the necessary policies, build definitions and release definitions are in
	 *  place for a new branch.
	 *  
	 *  @return Response
	 */

	public def handleNewBranch(def resourceData, def collection, def branchName) {
		//log.debug("PolicyManagementService::ensurePolicies -- resourceData =\n"+resourceData)
		log.debug("PolicyManagementService::handleNewBranch -- branchName = "+branchName)
		// first create the CI build policy
		def repoData = resourceData.repository
		//def project = repoData.project
		//ensureGitAttributesFile(collection, repoData)
		ensurePolicies(collection, repoData, branchName)
	}

	public def ensurePolicies(def collection, def repoData, def branchName, def policyData = null) {
		boolean enforceBuildValidation = true
		boolean enforceMinimumApprovers = true
		boolean enforceLinkedWorkItems = true
		boolean enforceMergeStrategy = true
		boolean enforceCommentResolution = true
		boolean isInitBranch = false
		
		//if (branchName.toLowerCase().equals("refs/heads/master")) return

		this.loadProperties(collection, repoData, branchName)
		// if we're dealing with an 'IFB' branch, check for custom branch build / policy configuration
		if (branchName.toLowerCase().startsWith("refs/heads/ifb") || branchName.toLowerCase().startsWith("refs/heads/feature/ifb")) {
			// See if branch participates in policy enforcement
			if (this.branchProps != null) {
				String enforcementFlag = this.branchProps.getProperty(ENFORCE_BUILD_VALIDATION)
				if (enforcementFlag != null && !enforcementFlag.equalsIgnoreCase("true")) {
					enforceBuildValidation = false
				}
				enforcementFlag = this.branchProps.getProperty(ENFORCE_MIN_APPROVERS)
				if (enforcementFlag != null && !enforcementFlag.equalsIgnoreCase("true")) {
					enforceMinimumApprovers = false
				}
				enforcementFlag = this.branchProps.getProperty(ENFORCE_LINKED_WI)
				if (enforcementFlag != null && !enforcementFlag.equalsIgnoreCase("true")) {
					enforceLinkedWorkItems = false
				}
				enforcementFlag = this.branchProps.getProperty(ENFORCE_MERGE_STRATEGY)
				if (enforcementFlag != null && !enforcementFlag.equalsIgnoreCase("true")) {
					enforceMergeStrategy = false
				}
				enforcementFlag = this.branchProps.getProperty(ENFORCE_COMMENT_RES)
				if (enforcementFlag != null && !enforcementFlag.equalsIgnoreCase("true")) {
					enforceCommentResolution = false
				}
			}
			// need to establish an initial build tag for ifb branch
			buildManagementService.ensureInitialTag(collection, repoData.project, repoData, branchName)
		}
		
		// Check for special 'adoinit' branch used to patch master branch build / policy configuration
		if (branchName.toLowerCase().equals("refs/heads/adoinit")) {
			log.debug("PolicyManagementService::ensurePolicies -- Found ADO init branch. Applying policies to master")
			branchName = "refs/heads/master"
			isInitBranch = true
		}

		log.debug("PolicyManagementService::ensurePolicies -- Build validation policy enforced = "+enforceBuildValidation)
		log.debug("PolicyManagementService::ensurePolicies -- Minimum approvers policy enforced = "+enforceMinimumApprovers)
		log.debug("PolicyManagementService::ensurePolicies -- Linked work items policy enforced = "+enforceLinkedWorkItems)
		log.debug("PolicyManagementService::ensurePolicies -- Merge strategy policy enforced = "+enforceMergeStrategy)
		log.debug("PolicyManagementService::ensurePolicies -- Comment resolution policy enforced = "+enforceCommentResolution)
		if (enforceBuildValidation) {
			// first create the CI build validation policy
			def buildData = null
			if (policyData && policyData.buildData) {
				buildData = policyData.buildData
			}
			ensureBuildPolicy(collection, repoData, branchName, isInitBranch, buildData)
		}
		// create other policies ...
		if (enforceMinimumApprovers) {
			def approvalData = null
			if (policyData && policyData.approvalData) {
				approvalData = policyData.approvalData
			}
			ensureMinimumApproversPolicy(collection, repoData, branchName, approvalData)
		}
		if (enforceLinkedWorkItems) {
			ensureLinkedWorkItemsPolicy(collection, repoData, branchName)
		}
		if (enforceMergeStrategy) {
			ensureMergeStrategyPolicy(collection, repoData, branchName)
		}
		if (enforceCommentResolution) {
			ensureCommentResolutionPolicy(collection, repoData, branchName)
		}

	}
	/**
	 *  This method creates and applies the CI build policy for validating code merges for new pull requests.
	 *  It also extends to create the CI and Release build definitions if they do not already exist.
	 *  
	 *  @return Response
	 */
	public def ensureBuildPolicy(def collection, def repoData, def branchName, boolean isInitBranch, def buildData = null) {

		// get the CI build
		def projectData = repoData.project
		def ciBuildTemplate = null
		def relBuildTemplate = null
		def yamlFile = null
		if (this.branchProps != null) {
			ciBuildTemplate = this.branchProps.getProperty(CI_BUILD_TEMPLATE)
			relBuildTemplate = this.branchProps.getProperty(RELEASE_BUILD_TEMPLATE)
			log.debug("PolicyManagementService::ensureBuildPolicy -- Specified CI build template = ${ciBuildTemplate}")
			log.debug("PolicyManagementService::ensureBuildPolicy -- Specified Release build template = ${relBuildTemplate}")
			yamlFile = this.branchProps.getProperty(CI_BUILD_YAML_FILE)
		}
		// check for specified yaml file name
		if (buildData == null) {
			if (yamlFile) {
				log.debug("PolicyManagementService::ensureBuildPolicy -- Specified YAML build file = ${yamlFile}")
				buildData = [ciBuildFile: "${yamlFile}"]
			}
		}
		// result is a JSON object
		def result = buildManagementService.ensureBuildsForBranch(collection, projectData, repoData, ciBuildTemplate, relBuildTemplate, isInitBranch, buildData)
		int ciBuildId = result.ciBuildId
		if (ciBuildId == -1) {
			log.debug("PolicyManagementService::ensureBuildPolicy -- No CI Build Definition was found or created. Unable to create the validation build policy!")
			return null
		}
		def pipelineName = "${repoData.name} validation"
		def policy = [id: -2, isBlocking: true, isDeleted: false, isEnabled: true, revision: 1,
		    type: [id: BUILD_VALIDATION_POLICY_TYPE],
		    settings:[buildDefinitionId: ciBuildId, displayName: pipelineName, manualQueueOnly: false, queueOnSourceUpdateOnly:true, validDuration: 720,
				scope:[[matchKind: 'Exact',refName: branchName, repositoryId: repoData.id]]
			]
		]
		// check for existing build validation policy
		def policyRes = getBranchPolicy(BUILD_VALIDATION_POLICY_TYPE, projectData.id, repoData.id, branchName)
		if (!policyRes) {
			policyRes = createPolicy(collection, projectData, policy)
		}
		log.debug("PolicyManagementService::ensureBuildPolicy -- result = "+policyRes)
		
		int relBuildId = result.releaseBuildId
		// create release definition for release build
		def relResult = null
		def relDefName = ""
		/*  SKIP creating release definition which we no longer use 
		if (relBuildId > -1) {
			def releaseTemplate = null
			// look for specified release template
			if (this.branchProps != null) {
				releaseTemplate = this.branchProps.getProperty("release-template")
				log.debug("PolicyManagementService::ensureBuildPolicy -- Specified Release template = ${releaseTemplate}")
			}
			log.debug("PolicyManagementService::ensureBuildPolicy -- Release Build Definition created. Will attempt to create a release definition")
			relResult = releaseManagementService.ensureReleaseForBuild(collection, projectData, repoData, relBuildId, releaseTemplate)
			// check status for release definition creation
			if (!relResult.relDefCreated && !relResult.relDefFound) {
				log.error("PolicyManagementService::ensureBuildPolicy -- Release Definition NOT found and failed creation")
			} else if (relResult.relDefCreated) {
				relDefName = relResult.releaseDefName
				log.debug("PolicyManagementService::ensureBuildPolicy -- Release Definition created: "+relDefName)
			}
		}*/
		
		// send email if builds were created
		if (result.ciBuildName != "" || result.releaseBuildName != "") {
			// send notification of new builds created
			notificationService.sendBuildCreatedNotification("${repoData.name}", result.ciBuildName, result.releaseBuildName, relDefName)
		}
		return policyRes
	}
	
	private def createPolicy(def collection, def projectData, def policy) {
		def body = new JsonBuilder(policy).toPrettyString()
		//log.debug("PolicyManagementService::createPolicy -- Request body = "+body)
		def result = genericRestClient.post(
				requestContentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectData.id}/_apis/policy/configurations",
				body: body,
				headers: [Accept: 'application/json;api-version=5.1;excludeUrls=true']
		)
		return result
	}

	public def updatePolicy(def collection, def projectData, def policy) {
		def body = new JsonBuilder(policy).toPrettyString()
		log.debug("PolicyManagementService::updatePolicy -- Request body = "+body)
		def result = genericRestClient.put(
				requestContentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectData.id}/_apis/policy/configurations/${policy.id}",
				body: body,
				headers: [Accept: 'application/json;api-version=6.0;excludeUrls=true']
		)
		return result
	}

	public def ensureMinimumApproversPolicy(def collection, def repoData, def branchName, def approvalData = null) {
		def policyRes
		def numMinApprovers = DEFAULT_NUM_APPROVERS
		boolean creatorVoteCounts = false
		boolean prohibitMostRecentPusher = true
		if (approvalData) {
			if (approvalData.minApprovers) {
				numMinApprovers = approvalData.minApprovers
			}
			if (approvalData.creatorVoteCounts != null) {
				creatorVoteCounts = approvalData.creatorVoteCounts
			}
			if (approvalData.prohibitMostRecentPusher != null) {
				prohibitMostRecentPusher = approvalData.prohibitMostRecentPusher
			}
		}
		def projectData = repoData.project
		// check for existing policy
		policyRes = getBranchPolicy(MIN_APPROVERS_POLICY_TYPE, projectData.id, repoData.id, branchName)
		if (!policyRes) {
			if (this.branchProps != null) {
				String tempNum = this.branchProps.getProperty(NUM_MIN_APPROVERS)
				if (tempNum != null && isNumeric(tempNum)) {
					numMinApprovers = Integer.parseInt(tempNum)
					// must have at least 1 approver
					if (numMinApprovers < DEFAULT_NUM_APPROVERS) numMinApprovers = DEFAULT_NUM_APPROVERS
					log.debug("PolicyManagementService::ensureMinimumApproversPolicy -- Number of minimum approvers = ${numMinApprovers}")
				}
			}
			log.debug("PolicyManagementService::ensureMinimumApproversPolicy -- ")
			def policy = [id: -3, isBlocking: true, isDeleted: false, isEnabled: true, revision: 1,
			    type: [id: MIN_APPROVERS_POLICY_TYPE],
			    settings:[minimumApproverCount: numMinApprovers, creatorVoteCounts: creatorVoteCounts, allowDownvotes: false, resetOnSourcePush: true, blockLastPusherVote: true,
					scope:[[matchKind: 'Exact',refName: branchName, repositoryId: repoData.id]]
				]
			]
			policyRes = createPolicy(collection, projectData, policy)
		} else {
			log.debug("Existing policy found.  Checking for changes ...")
			boolean valuesChanged = false
			if (policyRes.settings.minimumApproverCount != numMinApprovers) {
				log.debug("minimumApproverCount was changed to ${numMinApprovers}")
				policyRes.settings.minimumApproverCount = numMinApprovers
				valuesChanged = true
			}
			if (policyRes.settings.creatorVoteCounts != creatorVoteCounts) {
				log.debug("creatorVoteCounts was changed to ${creatorVoteCounts}")
				policyRes.settings.creatorVoteCounts = creatorVoteCounts
				valuesChanged = true
			}
			if (policyRes.settings.blockLastPusherVote != prohibitMostRecentPusher) {
				log.debug("blockLastPusherVote was changed to ${prohibitMostRecentPusher}")
				policyRes.settings.blockLastPusherVote = prohibitMostRecentPusher
				valuesChanged = true
			}
			// update policy for minimum approvers if changes were made
			if (valuesChanged) {
				log.debug("Changes were made.  Validating changes ...")
				boolean changesAreValid = true
				// validate changes before applying
				if (policyRes.settings.minimumApproverCount < 1 || policyRes.settings.minimumApproverCount > 10) {
					log.error("Invalid input for Miniumum number of reviewers.")
					changesAreValid = false
				} else if (policyRes.settings.minimumApproverCount == 1) {
					if (policyRes.settings.creatorVoteCounts != false) {
						log.error("Invalid input for Allow requestor to approve their own changes.")
						changesAreValid = false
					}
					if (policyRes.settings.blockLastPusherVote != true) {
						log.error("Invalid input for Prohibit most recent pusher from approving their own changes.")
						changesAreValid = false
					}
				// When minimum reviewers > 1, all combinations of the other 2, creatorVoteCounts and blockLastPusherVote are valid
				}
				if (changesAreValid) {
					log.debug("Changes are valid.  Updating policy ...")
					policyRes = updatePolicy(collection, projectData, policyRes)
				}
			}
		}

		log.debug("PolicyManagementService::ensureMinimumApproversPolicy -- result = "+policyRes)

		return policyRes
	}
	
	public def ensureLinkedWorkItemsPolicy(def collection, def repoData, def branchName) {
		def projectData = repoData.project
		log.debug("PolicyManagementService::ensureLinkedWorkItemsPolicy -- ")
		def policy = [id: -4, isBlocking: true, isDeleted: false, isEnabled: true, revision: 1,
		    type: [id: LINKED_WI_POLICY_TYPE],
		    settings:[
				scope:[[matchKind: 'Exact',refName: branchName, repositoryId: repoData.id]]
			]
		]
		def policyRes = getBranchPolicy(LINKED_WI_POLICY_TYPE, projectData.id, repoData.id, branchName)
		if (!policyRes) {
			policyRes = createPolicy(collection, projectData, policy)
		}
		log.debug("PolicyManagementService::ensureLinkedWorkItemsPolicy -- result = "+policyRes)

		return policyRes
	}
	
	public def ensureMergeStrategyPolicy(def collection, def repoData, def branchName) {
		def projectData = repoData.project
		log.debug("PolicyManagementService::ensureMergeStrategyPolicy -- ")
		def policy = [id: -5, isBlocking: true, isDeleted: false, isEnabled: true, revision: 1,
		    type: [id: MERGE_STRATEGY_POLICY_TYPE],
		    settings:[allowNoFastForward: true,
				scope:[[matchKind: 'Exact',refName: branchName, repositoryId: repoData.id]]
			]
		]
		def policyRes = getBranchPolicy(MERGE_STRATEGY_POLICY_TYPE, projectData.id, repoData.id, branchName)
		if (!policyRes) {
			policyRes = createPolicy(collection, projectData, policy)
		}
		log.debug("PolicyManagementService::ensureMergeStrategyPolicy -- result = "+policyRes)

		return policyRes
	}
	
	public def ensureCommentResolutionPolicy(def collection, def repoData, def branchName) {
		def projectData = repoData.project
		log.debug("PolicyManagementService::ensureCommentResolutionPolicy -- ")
		def policy = [id: -3, isBlocking: true, isDeleted: false, isEnabled: true, revision: 1,
		    type: [id: COMMENT_RES_POLICY_TYPE],
		    settings:[
				scope:[[matchKind: 'Exact',refName: branchName, repositoryId: repoData.id]]
			]
		]
		def policyRes = getBranchPolicy(COMMENT_RES_POLICY_TYPE, projectData.id, repoData.id, branchName)
		if (!policyRes) {
			policyRes = createPolicy(collection, projectData, policy)
		}
		log.debug("PolicyManagementService::ensureCommentResolutionPolicy -- result = "+policyRes)

		return policyRes
	}
	
	public def ensureAutomaticallyIncludedReviewersBranchPolicy(String collection, def repoData, String branchName, def automaticallyIncludedReviewersData = null) {
		log.info("PolicyManagementService::ensureAutomaticallyIncludedReviewersBranchPolicy -- ")
				
		// Create the collection of reviewer Ids first so we can use in policy lookup
		String[] rNames = automaticallyIncludedReviewersData.reviewers.split(',')
		boolean aGroupIsInTheReviewersList = false
		def reviewersById = []
		for (reviewer in rNames) {
			// replace the () in the reviewer names provided with [] and '/' with '\' due to constraints in yaml validation
			reviewer = reviewer.replace('(', '[').replace(')', ']').replace('/', '\'')
			// Get the reviewer's identity
			def identity = memberManagementService.getIdentity('', reviewer)
			// Determine if the reviewer is a Team
			if (identity[0].properties.SchemaClassName.$value.toLowerCase() == "group") {
				aGroupIsInTheReviewersList = true
			}
			reviewersById.add("${identity[0].id}")
		}

		def policyRes = getBranchPolicy(AUTOMATICALLY_INCLUDED_REVIEWERS_POLICY_TYPE, repoData.project.id, repoData.id, branchName, null, null, reviewersById)
		if (policyRes) {
			// Remove the existing AutomaticallyIncludedReviewers branch policy that was found
			// Should we try to update instead of delete and re-add??
			deleteAutomaticallyIncludedReviewersBranchPolicy(policyRes)
		}
		
		// policyRequirement - Default: optional
		boolean isBlocking = automaticallyIncludedReviewersData.policyRequirement

		// TBD: add code to log when YAML configuration is doing something the UI would not allow
		// minimumApproverCount - Default: 1 (always pass in the REST call)
		def minimumApproverCount = 1
		// If our 1 reviewer is a group and this is a required policy, set minimumApproverCount to the YAML value
		if (reviewersById.size() == 1 && aGroupIsInTheReviewersList && isBlocking) {
			if (automaticallyIncludedReviewersData.minimumApproverCount) {
				minimumApproverCount = automaticallyIncludedReviewersData.minimumApproverCount
			}
		}

		// TBD: add code to log when YAML configuration is doing something the UI would not allow
		// filenamePatterns - Default: empty list (always pass in the REST call)
		def filenamePatterns = []
		if (automaticallyIncludedReviewersData.filenamePatterns && automaticallyIncludedReviewersData.filenamePatterns != "n/a") {
			// We expect the YAML to be entered as comma-separated list of paths / patterns. 
			String[] fnPaths = automaticallyIncludedReviewersData.filenamePatterns.split(',')
			for (fP in fnPaths) {
				filenamePatterns.add(fP)
			}
		}

		// creatorVoteCounts - Default: false
		boolean creatorVoteCounts = automaticallyIncludedReviewersData.creatorVoteCounts
		
		// message - Default: empty string
		def message = ""
		if (automaticallyIncludedReviewersData.message && automaticallyIncludedReviewersData.message != "n/a") {
			message = item.message.toString()
		}

		def scope = [[matchKind: 'Exact', refName: branchName, repositoryId: repoData.id]]

		// Create the policy
		def settings = ["filenamePatterns": filenamePatterns, "creatorVoteCounts": creatorVoteCounts.toString(), "message": message, "minimumApproverCount": minimumApproverCount, "requiredReviewerIds": reviewersById, scope: scope]
		def policy = [isBlocking: isBlocking.toString(), isDeleted: false, isEnabled: true, settings: settings, type: [id: AUTOMATICALLY_INCLUDED_REVIEWERS_POLICY_TYPE]]
		policyRes = createPolicy(collection, repoData.project, policy)

	}
	
	def deleteAutomaticallyIncludedReviewersBranchPolicy(def policyRes) {
		def result = genericRestClient.delete(
			uri: "${policyRes._links.self.href}",
			query: ['api-version': '5.1'])
	}
	
	def ensureCheckmarxBranchPolicy(String collection, def projectData, def repoData, String branchName) {
		
		def policyRes = getBranchPolicy(CUSTOM_STATUS_POLICY_TYPE, projectData.id, repoData.id, branchName, SAST_STATUS_GENRE, CHECKMARX_STATUS_NAME)
		if (!policyRes) {
			def scope = [[matchKind: 'Exact', refName: branchName, repositoryId: repoData.id]]
			def identity = memberManagementService.getIdentity('', 'svc-cloud-vsbuildagent')
			def settings = [authorId: '3b34d159-9e69-630b-9700-d7643a4d0fee', defaultDisplayName: "Checkmarx scan request sent", invalidateOnSourceUpdate: false, policyApplicability: null, statusGenre: 'sast', statusName: 'checkmarx', scope: scope]
			if (identity.size() > 0) {				
				settings = [authorId: "${identity[0].id}", defaultDisplayName: "Checkmarx scan request sent", invalidateOnSourceUpdate: false, policyApplicability: null, statusGenre: 'sast', statusName: 'checkmarx', scope: scope]
			}
			def policy = [isBlocking: true, isDeleted: false, isEnabled: true, settings: settings, type: [id: CUSTOM_STATUS_POLICY_TYPE]]
			policyRes = createPolicy(collection, projectData, policy)
		}

	}
	
	def deleteCheckmarxBranchPolicy(def projectData, def repoData, String branchName = 'refs/heads/master') {
		def policyRes = getBranchPolicy(CUSTOM_STATUS_POLICY_TYPE, projectData.id, repoData.id, branchName, SAST_STATUS_GENRE, CHECKMARX_STATUS_NAME)
		if (policyRes) {
			def result = genericRestClient.delete(
				uri: "${policyRes._links.self.href}",
				query: ['api-version': '5.1'])
		}

	}

	public def ensureWatchdogBranchPolicy(def collection, def repoData, String branchName = 'refs/heads/master') {
		def policyRes
		def projectData = repoData.project
		// check for existing policy
		policyRes = getBranchPolicy(CUSTOM_STATUS_POLICY_TYPE, projectData.id, repoData.id, branchName, CI_STATUS_GENRE, WATCHDOG_STATUS_NAME)
		if (!policyRes) {
			log.debug("PolicyManagementService::ensureWatchdogBranchPolicy -- Adding custom status policy for ${CI_STATUS_GENRE}/${WATCHDOG_STATUS_NAME}")
			def policy = [id: -3, isBlocking: true, isDeleted: false, isEnabled: true, revision: 1,
			    type: [id: CUSTOM_STATUS_POLICY_TYPE],
			    settings:[statusName: WATCHDOG_STATUS_NAME, statusGenre: CI_STATUS_GENRE, invalidateOnSourceUpdate: true, defaultDisplayName: 'Watchdog status',
					scope:[[matchKind: 'Exact', refName: branchName, repositoryId: repoData.id]]
				]
			]
			policyRes = createPolicy(collection, projectData, policy)
		}

		log.debug("PolicyManagementService::ensureWatchdogBranchPolicy -- result = "+policyRes)

		return policyRes
	}
	
	public def ensureSNowCIBranchPolicy(def collection, def repoData, String branchName = 'refs/heads/master') {
		def policyRes
		def projectData = repoData.project
		// check for existing policy
			log.debug("Checking for existing custom status policy for ${CI_STATUS_GENRE}/${SNOWCI_STATUS_NAME}")
		policyRes = getBranchPolicy(CUSTOM_STATUS_POLICY_TYPE, projectData.id, repoData.id, branchName, CI_STATUS_GENRE, SNOWCI_STATUS_NAME)
		if (!policyRes) {
			log.debug("PolicyManagementService::ensureSNowCIBranchPolicy -- Adding custom status policy for ${CI_STATUS_GENRE}/${SNOWCI_STATUS_NAME}")
			def policy = [id: -3, isBlocking: true, isDeleted: false, isEnabled: true, revision: 1,
			    type: [id: CUSTOM_STATUS_POLICY_TYPE],
			    settings:[statusName: SNOWCI_STATUS_NAME, statusGenre: CI_STATUS_GENRE, invalidateOnSourceUpdate: true, defaultDisplayName: 'SNow CI status',
					scope:[[matchKind: 'Exact', refName: branchName, repositoryId: repoData.id]]
				]
			]
			policyRes = createPolicy(collection, projectData, policy)
		}

		log.debug("PolicyManagementService::ensureSNowCIBranchPolicy -- result = "+policyRes)

		return policyRes
	}
	
	public def ensureGitAttributesFile(def collection, def repoData) {
		log.debug("PolicyManagementService::ensureGitAttributesFile -- ")
		def res = codeManagementService.ensureGitAttributes(collection, repoData.project, repoData)
		log.debug("PolicyManagementService::ensureGitAttributesFile -- result = "+res)
		
		return res
	}
	
	public def modifyBranchPolicies(def collection, def repoData, def branchName, def policyData = null) {
		log.info("PolicyManagementService::modifyBranchPolicies -- Started")
		
		if (policyData) {
			def approvalData = null
			if (policyData.approvalData) {
				log.debug("PolicyManagementService::modifyBranchPolicies -- approvalData")
				approvalData = policyData.approvalData
				ensureMinimumApproversPolicy(collection, repoData, branchName, approvalData)
			}
		
			//def buildData = null
			//if (policyData.buildData) {
			//	log.info("PolicyManagementService::modifyBranchPolicies -- buildData")
			//	buildData = policyData.buildData
			//    // Need new code to handle collections of build validation policies
			//	// YAML usage in the wild today
			//	//   1) single CI YAML that has conditionals within the YAML on build.reason to control PR vs Release build (legacy)
			//	//   2) YAML for CI, Release and Dev builds (blueprint)
			//	//   3) YAML to meet singular pipelines (custom)
			//}
		
			def automaticallyIncludedReviewersData = null
			if (policyData.automaticallyIncludedReviewersData) {
				log.debug("PolicyManagementService::modifyBranchPolicies -- automaticallyIncludedReviewersData")
				automaticallyIncludedReviewersData = policyData.automaticallyIncludedReviewersData
				ensureAutomaticallyIncludedReviewersBranchPolicy(collection, repoData, branchName, automaticallyIncludedReviewersData)
			}
		}
	}

	public def getBranchPolicyReport(def collection, def project) {
		log.debug("PolicyManagementService::getBranchPolicyReport -- Started")
		def policyReport = [repos: []]
		def repoColl = []
		def repos = codeManagementService.getRepos(collection, project)
		repos.value.each { repo ->
			def repoObj = [repoName: "${repo.name}"]
			repoObj.repoId = "${repo.id}"
			repoObj.repoURL = "${repo.url}"
			def branchColl = []
			repoObj.branches = branchColl
			def branches = codeManagementService.getBranchesForReport(collection, project, repo)
			//def branches = codeManagementService.getBranches(collection, "${project.id}", "${repo.id}")
			if (branches == null) {
				// error or do not permission
				log.info("An error occurred or no permissions to get branches for ${repo.name}...")
			} else {
				branches.value.each { branch ->
					String branchName = "${branch.name}".toLowerCase()
					if (branchName.startsWith("refs/heads/master") || 
						branchName.startsWith("refs/heads/release") ||
						branchName.startsWith("refs/heads/feature/ifb") ||
						branchName.startsWith("refs/heads/ifb/"))
					{
						String bName = "${branch.name}".substring("refs/heads/".length())
						def branchObj = [branchName: bName]
						
						def policyObj = [
								hasBuildPolicy: false,
								hasMinimumReviewersPolicy: false,
								hasLinkedWorkItemsPolicy: false,
								hasMergeStrategyPolicy: false,
								hasCommentResolutionPolicy: false]
						// check for policies on branch
						log.debug("PolicyManagementService::getBranchPolicyReport -- Getting branch policies for branch " + bName + " ...")
						def policies = getBranchPolicies(project, repo.id, branchName)
						policies.value.each { policy ->
							// check for build validation policy
							if ("${policy.type.id}" == "0609b952-1397-4640-95ec-e00a01b2c241") {
								policyObj.hasBuildPolicy = true
								// get build def for CI build and 
								def build = buildManagementService.getBuildById(collection, project, policy.settings.buildDefinitionId)
								if (build == null) {
									log.error("Build definition (ID) " + policy.settings.buildDefinitionId + " for build validation policy NOT FOUND.")
									policyObj.ciBuildName = "Build Def NOT FOUND"
								} else {
									policyObj.ciBuildName = "${build.name}"
								}
							} else
							if ("${policy.type.id}" == "fa4e907d-c16b-4a4c-9dfa-4906e5d171dd") {
								policyObj.hasMinimumReviewersPolicy = true
								policyObj.minimumNumReviewers = policy.settings.minimumApproverCount
								policyObj.creatorCanApprove = policy.settings.creatorVoteCounts
								policyObj.prohibitMostRecentPusherApproval = policy.settings.blockLastPusherVote
								policyObj.allowDownvotes = policy.settings.allowDownvotes
								policyObj.resetIfChanged = policy.settings.resetOnSourcePush
								if (policyObj.resetIfChanged) {
									policyObj.requireAtLeastOneApprover = policy.settings.requireVoteOnLastIteration
									policyObj.resetAllVotes = policy.settings.resetRejectionsOnSourcePush
									policyObj.resetApprovals = (!policy.settings.resetRejectionsOnSourcePush && !policy.settings.requireVoteOnLastIteration)
								}
							} else
							if ("${policy.type.id}" == "40e92b44-2fe1-4dd6-b3d8-74a9c21d0c6e") {
								policyObj.hasLinkedWorkItemsPolicy = true
							} else
							if ("${policy.type.id}" == "fa4e907d-c16b-4a4c-9dfa-4916e5d171ab") {
								policyObj.hasMergeStrategyPolicy = true
								// what's the merge strategy -- NOT in settings ??
								if (policy.settings.allowNoFastForward) {
									policyObj.allowNoFastForward = policy.settings.allowNoFastForward
								} else {
									policyObj.allowNoFastForward = false
								}
								if (policy.settings.allowSquash) {
									policyObj.allowSquash = policy.settings.allowSquash
								} else {
									policyObj.allowSquash = false
								}
								if (policy.settings.allowRebase) {
									policyObj.allowRebase = policy.settings.allowRebase
								} else {
									policyObj.allowRebase = false
								}
								if (policy.settings.allowRebaseMerge) {
									policyObj.allowRebaseMerge = policy.settings.allowRebaseMerge
								} else {
									policyObj.allowRebaseMerge = false
								}
							} else
							if ("${policy.type.id}" == "c6a1889d-b943-4856-b76f-9e46bb6b0df2") {
								policyObj.hasCommentResolutionPolicy = true
							}
						}
						branchObj.policyInfo = policyObj
						branchColl.add(branchObj)
					}
				}
			}
			repoColl.add(repoObj)
		}
		policyReport.repos = repoColl

		log.debug("PolicyManagementService::getBranchPolicyReport -- Done")
		return policyReport
	}

	public def getPolicyExceptionReport(def collection, def project) {
		log.debug("PolicyManagementService::getPolicyExceptionReport -- Started")
		def policyReport = [repos: []]
		def repoColl = []
		def repos = codeManagementService.getRepos(collection, project)
		repos.value.each { repo ->
			def repoObj = [repoName: "${repo.name}"]
			repoObj.repoId = "${repo.id}"
			repoObj.repoURL = "${repo.url}"
			def branchColl = []
			repoObj.branches = branchColl
			def branches = codeManagementService.getBranchesForReport(collection, project, repo)
			//def branches = codeManagementService.getBranches(collection, "${project.id}", "${repo.id}")
			if (branches == null) {
				// error or do not permission
				log.info("An error occurred or no permissions to get branches for ${repo.name}...")
			} else {
				branches.value.each { branch ->
					String branchName = "${branch.name}".toLowerCase()
					if (branchName.startsWith("refs/heads/master") || 
						branchName.startsWith("refs/heads/release"))
					{
						String bName = "${branch.name}".substring("refs/heads/".length())
						def branchObj = [branchName: bName]
						
						def policyObj = [
								hasBuildPolicy: false,
								hasMinimumReviewersPolicy: false,
								hasLinkedWorkItemsPolicy: false,
								hasMergeStrategyPolicy: false,
								hasCommentResolutionPolicy: false]
						// check for policies on branch
						log.debug("PolicyManagementService::getPolicyExceptionReport -- Getting branch policies for branch " + bName + " ...")
						def policies = getBranchPolicies(project, repo.id, branchName)
						policies.value.each { policy ->
							// check for build validation policy
							if ("${policy.type.id}" == "0609b952-1397-4640-95ec-e00a01b2c241") {
								policyObj.hasBuildPolicy = true
								// get build def for CI build and 
								def build = buildManagementService.getBuildById(collection, project, policy.settings.buildDefinitionId)
								if (build == null) {
									//
								} else {
									policyObj.ciBuildName = "${build.name}"
								}
							} else
							if ("${policy.type.id}" == "fa4e907d-c16b-4a4c-9dfa-4906e5d171dd") {
								policyObj.hasMinimumReviewersPolicy = true
								policyObj.minimumNumReviewers = policy.settings.minimumApproverCount
								policyObj.creatorCanApprove = policy.settings.creatorVoteCounts
								policyObj.prohibitMostRecentPusherApproval = policy.settings.blockLastPusherVote
								policyObj.allowDownvotes = policy.settings.allowDownvotes
								policyObj.resetIfChanged = policy.settings.resetOnSourcePush
								if (policyObj.resetIfChanged) {
									policyObj.requireAtLeastOneApprover = policy.settings.requireVoteOnLastIteration
									policyObj.resetAllVotes = policy.settings.resetRejectionsOnSourcePush
									policyObj.resetApprovals = (!policy.settings.resetRejectionsOnSourcePush && !policy.settings.requireVoteOnLastIteration)
								}
							} else
							if ("${policy.type.id}" == "40e92b44-2fe1-4dd6-b3d8-74a9c21d0c6e") {
								policyObj.hasLinkedWorkItemsPolicy = true
							} else
							if ("${policy.type.id}" == "fa4e907d-c16b-4a4c-9dfa-4916e5d171ab") {
								policyObj.hasMergeStrategyPolicy = true
								// what's the merge strategy -- NOT in settings ??
								if (policy.settings.allowNoFastForward) {
									policyObj.allowNoFastForward = policy.settings.allowNoFastForward
								} else {
									policyObj.allowNoFastForward = false
								}
								if (policy.settings.allowSquash) {
									policyObj.allowSquash = policy.settings.allowSquash
								} else {
									policyObj.allowSquash = false
								}
								if (policy.settings.allowRebase) {
									policyObj.allowRebase = policy.settings.allowRebase
								} else {
									policyObj.allowRebase = false
								}
								if (policy.settings.allowRebaseMerge) {
									policyObj.allowRebaseMerge = policy.settings.allowRebaseMerge
								} else {
									policyObj.allowRebaseMerge = false
								}
							} else
							if ("${policy.type.id}" == "c6a1889d-b943-4856-b76f-9e46bb6b0df2") {
								policyObj.hasCommentResolutionPolicy = true
							}
						}
						branchObj.policyInfo = policyObj
						branchColl.add(branchObj)
					}
				}
			}
			repoColl.add(repoObj)
		}
		policyReport.repos = repoColl

		log.debug("PolicyManagementService::getPolicyExceptionReport -- Done")
		return policyReport
	}

	public def getBranchPolicies( def project, def repoId, def branchName ) {
		log.debug("PolicyManagementService::getBranchPolicies -- Get policies for branch ${branchName}")
		def query = ['repositoryId':"${repoId}", 'refName':"${branchName}", 'api-version': '5.1-preview' ]
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${project.id}/_apis/git/policy/configurations",
				query: query
				//headers: [Accept: 'application/json;api-version=5.1;excludeUrls=true']
		)
		return result
	}

	def getBranchPolicy( def policyType, def projectId, def repoId, def branchName, def statusGenre=null, def statusName=null, def reviewerIds=null ) {
		def query = ['repositoryId':"${repoId}", 'refName':"${branchName}" , 'policyType':"${policyType}" ]
		def results = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${projectId}/_apis/git/policy/configurations",
			query: query,
			headers: [Accept: 'application/json;api-version=5.0-preview.1']
		)
		def retVal = null	
		results.'value'.each { policy ->
			// Need to verify isEnabled
			if (policy.isEnabled) {
				if (policyType == CUSTOM_STATUS_POLICY_TYPE) {
					if (policy.settings.statusName == statusName && policy.settings.statusGenre == statusGenre) {
						retVal = policy
						log.debug("PolicyManagementService::getBranchPolicy -- Found existing custom status policy for ${statusGenre}/${statusName}")
					}
				} else if (policyType == AUTOMATICALLY_INCLUDED_REVIEWERS_POLICY_TYPE) {
					// should we loop through and compare the set of reviewers
					List<String> requiredReviewerIds = policy.settings.requiredReviewerIds
					boolean reviewersExist = true
					for (String reviewerId in reviewerIds) {
						if (!requiredReviewerIds.contains(reviewerId)) {
							log.debug("Required reviewer with Id ${reviewerId} not found.")
							reviewersExist = false
						}
					}
					if (reviewersExist) {
						retVal = policy
						log.debug("PolicyManagementService::getBranchPolicy -- Found existing automatically included reviewers policy for ${reviewerIds}")
					}
				} else {
					retVal = policy
					log.debug("PolicyManagementService::getBranchPolicy -- Found existing branch policy for ${policyType}")
				}
			}
		}
		return retVal
	}
	
	private loadProperties(def collection, def repoData, String branchName) {
		def branch = branchName.substring("refs/heads/".length())
		log.debug("PolicyManagementService::loadProperties -- Get build properties for branch ${branch}")
		// initialize branch properties instance
		this.branchProps = null
		def buildPropertiesFile = codeManagementService.getFileContent(collection, repoData.project, repoData, buildPropsFileName, branch)
		if (buildPropertiesFile != null) {
			// load properties from file
			String fileContent = buildPropertiesFile.toString()
			this.branchProps = new java.util.Properties()
			this.branchProps.load(new java.io.StringBufferInputStream(fileContent))
		}
	}

	private static boolean isNumeric(String strNum) {
		try {
			double d = Double.parseDouble(strNum);
		} catch (NumberFormatException | NullPointerException nfe) {
			return false;
		}
		return true;
	}
	
	public def clearBranchPolicies(def collection, def project, def repoId, def branchName) {
		def policies = getBranchPolicies(project, repoId, branchName)
		if (!policies) return []
		policies.value.each { policy ->
			def result = genericRestClient.delete(
				uri: "${policy._links.self.href}",
				query: ['api-version': '5.1'])
		}
		return policies
	}
	
	public def restoreBranchPolicies(def collection, def project, def repoId, def branchName, def policies) {
		if (!policies.value) return policies
		policies.value.each { policy ->
			def policyR = [:]
			policyR.isEnabled = policy.isEnabled
			policyR.isBlocking = policy.isBlocking
			policyR.type = policy.type
			policyR.settings = policy.settings
			def result = createPolicy(collection, project, policyR)
		}
		def rpolicies = getBranchPolicies( project, repoId, branchName)
		
		return rpolicies
	}
}

