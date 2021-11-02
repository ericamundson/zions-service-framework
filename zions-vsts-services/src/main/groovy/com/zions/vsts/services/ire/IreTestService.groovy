package com.zions.vsts.services.ire;

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import groovy.util.logging.Slf4j
import groovy.json.JsonBuilder;
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType

import com.zions.common.services.notification.NotificationService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.build.BuildManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.release.ReleaseManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient

/**
 * 
 * @author James McNabb
 *
 */
@Component
@Slf4j
public class IreTestService {

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
	
	// ADO policy types
	private static final String BUILD_VALIDATION_POLICY_TYPE = "0609b952-1397-4640-95ec-e00a01b2c241"
	private static final String MIN_APPROVERS_POLICY_TYPE = "fa4e907d-c16b-4a4c-9dfa-4906e5d171dd"
	private static final String LINKED_WI_POLICY_TYPE = "40e92b44-2fe1-4dd6-b3d8-74a9c21d0c6e"
	private static final String COMMENT_RES_POLICY_TYPE = "c6a1889d-b943-4856-b76f-9e46bb6b0df2"
	private static final String MERGE_STRATEGY_POLICY_TYPE = "fa4e907d-c16b-4a4c-9dfa-4916e5d171ab"
	private static final int DEFAULT_NUM_APPROVERS = 1
	
	public IreTestService() {
	}

	/**
	 *  This method handles ensuring that all the necessary policies, build definitions and release definitions are in
	 *  place for a new DR branch during an IRE.
	 *  
	 *  @return Response
	 */

	public def handleDRBranch(def resourceData, def collection, def branchName) {
		//log.debug("IreTestService::handleDRBranch -- resourceData =\n"+resourceData)
		log.debug("IreTestService::handleDRBranch -- branchName = "+branchName)
		// first create the CI build policy
		def repoData = resourceData.repository
		//def project = repoData.project
		//ensureGitAttributesFile(collection, repoData)
		ensurePolicies(collection, repoData, branchName)
	}

	public def ensurePolicies(def collection, def repoData, def branchName, def policyData = null) {
		// TODO: Need to change this to get the policies for the master branch and duplicate them for DR branch
		boolean enforceBuildValidation = true
		boolean enforceMinimumApprovers = true
		boolean enforceLinkedWorkItems = true
		boolean enforceMergeStrategy = true
		boolean enforceCommentResolution = true

		if (enforceBuildValidation) {
			// first create the CI build validation policy
			def buildData = null
			if (policyData && policyData.buildData) {
				buildData = policyData.buildData
			}
			ensureBuildPolicy(collection, repoData, branchName, buildData)
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
	public def ensureBuildPolicy(def collection, def repoData, def branchName, def buildData = null) {
		
		// get the CI build
		def projectData = repoData.project
		// indicate DR branch
		boolean isDRBranch = true
		def ciBuildTemplate = null
		def relBuildTemplate = null
		def yamlFile = null

		// TODO: how do we check for yaml build or file name??
		if (buildData == null) {
			if (yamlFile) {
				log.debug("IreTestService::ensureBuildPolicy -- Specified YAML build file = ${yamlFile}")
				buildData = [ciBuildFile: "${yamlFile}"]
			}
		}
		// result is a JSON object
		def result = buildManagementService.ensureDRBuilds(collection, projectData, repoData)
		int ciBuildId = result.ciBuildId
		if (ciBuildId == -1) {
			log.debug("IreTestService::ensureBuildPolicy -- No CI Build Definition was found or created. Unable to create the validation build policy!")
			return null
		}
		def pipelineName = "${repoData.name} DR validation"
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
		log.debug("IreTestService::ensureBuildPolicy -- result = "+policyRes)
		
		int relBuildId = result.releaseBuildId
		// create release definition for release build
		def relResult = null
		def relDefName = ""
		if (relBuildId > -1) {
			def releaseTemplate = null
			log.debug("IreTestService::ensureBuildPolicy -- Release Build Definition created. Will attempt to create a release definition")
			relResult = releaseManagementService.ensureDRRelease(collection, projectData, repoData, relBuildId)
			// check status for release definition creation
			if (!relResult.relDefCreated && !relResult.relDefFound) {
				log.error("IreTestService::ensureBuildPolicy -- Release Definition NOT found and failed creation")
			} else if (relResult.relDefCreated) {
				relDefName = relResult.releaseDefName
				relDefName = "N/A"
				log.debug("IreTestService::ensureBuildPolicy -- Release Definition created: "+relDefName)
			}
		}
		
		// send email if builds were created
		if (result.ciBuildName != "" || result.releaseBuildName != "") {
			// send notification of new builds created
			notificationService.sendBuildCreatedNotification("${repoData.name}", result.ciBuildName, result.releaseBuildName, relDefName)
		}
		return policyRes
	}
	
	private def createPolicy(def collection, def projectData, def policy) {
		def body = new JsonBuilder(policy).toPrettyString()
		//log.debug("IreTestService::createPolicy -- Request body = "+body)
		def result = genericRestClient.post(
				requestContentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectData.id}/_apis/policy/configurations",
				body: body,
				headers: [Accept: 'application/json;api-version=5.1;excludeUrls=true']
		)
		return result
	}

	public def ensureMinimumApproversPolicy(def collection, def repoData, def branchName, def approvalData = null) {
		def projectData = repoData.project
		def numMinApprovers = DEFAULT_NUM_APPROVERS
		if (approvalData && approvalData.minApprovers) {
			numMinApprovers = approvalData.minApprovers
		}
		boolean creatorVoteCounts = false
		if (approvalData && approvalData.creatorVoteCounts) {
			creatorVoteCounts = approvalData.creatorVoteCounts
		}
		log.debug("IreTestService::ensureMinimumApproversPolicy -- ")
		def policy = [id: -3, isBlocking: true, isDeleted: false, isEnabled: true, revision: 1,
		    type: [id: MIN_APPROVERS_POLICY_TYPE],
		    settings:[minimumApproverCount: numMinApprovers, creatorVoteCounts: creatorVoteCounts, allowDownvotes: false, resetOnSourcePush: true,
				scope:[[matchKind: 'Exact',refName: branchName, repositoryId: repoData.id]]
			]
		]
		def policyRes = getBranchPolicy(MIN_APPROVERS_POLICY_TYPE, projectData.id, repoData.id, branchName)
		if (!policyRes) {
			policyRes = createPolicy(collection, projectData, policy)
		} else {
			// update for minimum approvers??
		}
		log.debug("IreTestService::ensureMinimumApproversPolicy -- result = "+policyRes)

		return policyRes
	}
	
	public def ensureLinkedWorkItemsPolicy(def collection, def repoData, def branchName) {
		def projectData = repoData.project
		log.debug("IreTestService::ensureLinkedWorkItemsPolicy -- ")
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
		log.debug("IreTestService::ensureLinkedWorkItemsPolicy -- result = "+policyRes)

		return policyRes
	}
	
	public def ensureMergeStrategyPolicy(def collection, def repoData, def branchName) {
		def projectData = repoData.project
		log.debug("IreTestService::ensureMergeStrategyPolicy -- ")
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
		log.debug("IreTestService::ensureMergeStrategyPolicy -- result = "+policyRes)

		return policyRes
	}
	
	public def ensureCommentResolutionPolicy(def collection, def repoData, def branchName) {
		def projectData = repoData.project
		log.debug("IreTestService::ensureCommentResolutionPolicy -- ")
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
		log.debug("IreTestService::ensureCommentResolutionPolicy -- result = "+policyRes)

		return policyRes
	}

	def getBranchPolicy( def policyType, def projectId, def repoId, def branchName ) {
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
				retVal = policy
				System.out.println("IreTestService::getBranchPolicy -- Found existing branch policy for ${policyType}")
				log.info("IreTestService::getBranchPolicy -- Found existing branch policy for ${policyType}")
				return
			}
		}
		return retVal
	}
	
	
}

