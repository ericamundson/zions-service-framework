package com.zions.vsts.services.policy;

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import groovy.util.logging.Slf4j
import groovy.json.JsonBuilder;
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType

import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.build.BuildManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.release.ReleaseManagementService
import com.zions.vsts.services.notification.NotificationService
import com.zions.vsts.services.tfs.rest.GenericRestClient

/**
 * 
 * @author James McNabb
 *
 */
@Component
@Slf4j
public class PolicyManagementConfiguredService {

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
	NotificationService notificationService
	
	private java.util.Properties branchProps

	private static final String ENFORCE_BUILD_VALIDATION = "enforce-build-validation"
	private static final String ENFORCE_MIN_APPROVERS = "enforce-minimum-approvers"
	private static final String ENFORCE_LINKED_WI = "enforce-linked-workitems"
	private static final String ENFORCE_COMMENT_RES = "enforce-comment-resolution"
	private static final String ENFORCE_MERGE_STRATEGY = "enforce-merge-strategy"
	private static final String NUM_MIN_APPROVERS = "num-min-reviewers"
	private static final int DEFAULT_NUM_APPROVERS = 1
	
	public PolicyManagementConfiguredService() {
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

	public def ensurePolicies(def collection, def repoData, def branchName) {
		boolean enforceBuildValidation = true
		boolean enforceMinimumApprovers = true
		boolean enforceLinkedWorkItems = true
		boolean enforceMergeStrategy = true
		boolean enforceCommentResolution = true

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
		
		log.debug("PolicyManagementService::ensurePolicies -- Build validation policy enforced = "+enforceBuildValidation)
		log.debug("PolicyManagementService::ensurePolicies -- Minimum approvers policy enforced = "+enforceMinimumApprovers)
		log.debug("PolicyManagementService::ensurePolicies -- Linked work items policy enforced = "+enforceLinkedWorkItems)
		log.debug("PolicyManagementService::ensurePolicies -- Merge strategy policy enforced = "+enforceMergeStrategy)
		log.debug("PolicyManagementService::ensurePolicies -- Comment resolution policy enforced = "+enforceCommentResolution)
		if (enforceBuildValidation) {
			// first create the CI build validation policy
			ensureBuildPolicy(collection, repoData, branchName)
		}
		// create other policies ...
		if (enforceMinimumApprovers) {
			ensureMinimumApproversPolicy(collection, repoData, branchName)
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
	public def ensureBuildPolicy(def collection, def repoData, def branchName) {
		
		// get the CI build
		def projectData = repoData.project
		// check for DR branch
		boolean isDRBranch = ("${branchName}".toLowerCase().startsWith("refs/heads/dr/"))
		def ciBuildTemplate = null
		def relBuildTemplate = null
		if (!isDRBranch) {
			if (this.branchProps != null) {
				ciBuildTemplate = this.branchProps.getProperty("build-template-ci")
				relBuildTemplate = this.branchProps.getProperty("build-template-release")
				log.debug("PolicyManagementService::ensureBuildPolicy -- Specified CI build template = ${ciBuildTemplate}")
				log.debug("PolicyManagementService::ensureBuildPolicy -- Specified Release build template = ${relBuildTemplate}")
			}
		}

		// result is a JSON object
		def result = buildManagementService.ensureBuildsForBranch(collection, projectData, repoData, isDRBranch, ciBuildTemplate, relBuildTemplate)
		int ciBuildId = result.ciBuildId
		if (ciBuildId == -1) {
			log.debug("PolicyManagementService::ensureBuildPolicy -- No CI Build Definition was found or created. Unable to create the validation build policy!")
			return null
		}
		def pipelineName = isDRBranch ? "${repoData.name} DR validation" : "${repoData.name} validation"
		def policy = [id: -2, isBlocking: true, isDeleted: false, isEnabled: true, revision: 1,
		    type: [id: "0609b952-1397-4640-95ec-e00a01b2c241"],
		    settings:[buildDefinitionId: ciBuildId, displayName: pipelineName, manualQueueOnly: false, queueOnSourceUpdateOnly:true, validDuration: 720,
				scope:[[matchKind: 'Exact',refName: branchName, repositoryId: repoData.id]]
			]
		]
		def res = createPolicy(collection, projectData, policy)
		log.debug("PolicyManagementService::ensureBuildPolicy -- result = "+res)
		
		int relBuildId = result.releaseBuildId
		// create release definition for release build
		def relResult = null
		def relDefName = ""
		if (relBuildId > -1) {
			def releaseTemplate = null
			// look for specified release template
			if (!isDRBranch) {
				if (this.branchProps != null) {
					releaseTemplate = this.branchProps.getProperty("release-template")
					log.debug("PolicyManagementService::ensureBuildPolicy -- Specified Release template = ${releaseTemplate}")
				}
			}
			log.debug("PolicyManagementService::ensureBuildPolicy -- Release Build Definition created. Will attempt to create a release definition")
			relResult = releaseManagementService.ensureReleaseForBuild(collection, projectData, repoData, relBuildId, isDRBranch, releaseTemplate)
			// check status for release definition creation
			if (!relResult.relDefCreated && !relResult.relDefFound) {
				log.error("PolicyManagementService::ensureBuildPolicy -- Release Definition NOT found and failed creation")
			} else if (relResult.relDefCreated) {
				relDefName = relResult.releaseDefName
				log.debug("PolicyManagementService::ensureBuildPolicy -- Release Definition created: "+relDefName)
			}
		}
		
		// send email if builds were created
		if (result.ciBuildName != "" || result.releaseBuildName != "") {
			// send notification of new builds created
			notificationService.sendBuildCreatedNotification("${repoData.name}", result.ciBuildName, result.releaseBuildName, relDefName)
		}
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

	public def ensureMinimumApproversPolicy(def collection, def repoData, def branchName) {
		def projectData = repoData.project
		def numMinApprovers = DEFAULT_NUM_APPROVERS
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
		    type: [id: "fa4e907d-c16b-4a4c-9dfa-4906e5d171dd"],
		    settings:[minimumApproverCount: numMinApprovers, creatorVoteCounts: false, allowDownvotes: false, resetOnSourcePush: true,
				scope:[[matchKind: 'Exact',refName: branchName, repositoryId: repoData.id]]
			]
		]
		def res = createPolicy(collection, projectData, policy)
		log.debug("PolicyManagementService::ensureMinimumApproversPolicy -- result = "+res)
	}
	
	public def ensureLinkedWorkItemsPolicy(def collection, def repoData, def branchName) {
		def projectData = repoData.project
		log.debug("PolicyManagementService::ensureLinkedWorkItemsPolicy -- ")
		def policy = [id: -4, isBlocking: true, isDeleted: false, isEnabled: true, revision: 1,
		    type: [id: "40e92b44-2fe1-4dd6-b3d8-74a9c21d0c6e"],
		    settings:[
				scope:[[matchKind: 'Exact',refName: branchName, repositoryId: repoData.id]]
			]
		]
		def res = createPolicy(collection, projectData, policy)
		log.debug("PolicyManagementService::ensureLinkedWorkItemsPolicy -- result = "+res)
	}
	
	public def ensureMergeStrategyPolicy(def collection, def repoData, def branchName) {
		def projectData = repoData.project
		log.debug("PolicyManagementService::ensureMergeStrategyPolicy -- ")
		def policy = [id: -5, isBlocking: true, isDeleted: false, isEnabled: true, revision: 1,
		    type: [id: "fa4e907d-c16b-4a4c-9dfa-4916e5d171ab"],
		    settings:[useSquashMerge: false,
				scope:[[matchKind: 'Exact',refName: branchName, repositoryId: repoData.id]]
			]
		]
		def res = createPolicy(collection, projectData, policy)
		log.debug("PolicyManagementService::ensureMergeStrategyPolicy -- result = "+res)
	}
	
	public def ensureCommentResolutionPolicy(def collection, def repoData, def branchName) {
		def projectData = repoData.project
		log.debug("PolicyManagementService::ensureCommentResolutionPolicy -- ")
		def policy = [id: -3, isBlocking: true, isDeleted: false, isEnabled: true, revision: 1,
		    type: [id: "c6a1889d-b943-4856-b76f-9e46bb6b0df2"],
		    settings:[
				scope:[[matchKind: 'Exact',refName: branchName, repositoryId: repoData.id]]
			]
		]
		def res = createPolicy(collection, projectData, policy)
		log.debug("PolicyManagementService::ensureCommentResolutionPolicy -- result = "+res)
	}

	public def ensureGitAttributesFile(def collection, def repoData) {
		log.debug("PolicyManagementService::ensureGitAttributesFile -- ")
		def res = codeManagementService.ensureGitAttributes(collection, repoData.project, repoData)
		log.debug("PolicyManagementService::ensureGitAttributesFile -- result = "+res)
	}


	private loadProperties(def collection, def repoData, def branchName) {
		def branch = "${branchName}".substring("refs/heads/".length())
		log.debug("PolicyManagementService::loadProperties -- Get build properties for branch ${branch}")
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
}

