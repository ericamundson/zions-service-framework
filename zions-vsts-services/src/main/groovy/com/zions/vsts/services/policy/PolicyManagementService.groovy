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

/**
 * 
 * @author James McNabb
 *
 */
@Component
@Slf4j
public class PolicyManagementService {

	@Autowired
	@Value('${tfs.build.properties.file}')
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

	private static final String ENFORCE_BUILD_POLICY = "enforce-build-policy"

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
		ensurePolicies(collection, repoData, branchName)
	}

	public def ensurePolicies(def collection, def repoData, def branchName) {
		def branch = "${branchName}".substring("refs/heads/".length())
		log.debug("PolicyManagementService::ensurePolicies -- Get build properties for branch ${branch}")
		def buildPropertiesFile = codeManagementService.getBuildPropertiesFile(collection, repoData.project, repoData, buildPropsFileName, branch)
		if (buildPropertiesFile != null) {
			// load properties from file
			String fileContent = buildPropertiesFile.toString()
			this.branchProps = new java.util.Properties()
			this.branchProps.load(new java.io.StringBufferInputStream(fileContent))
		}

		// first create the CI build validation policy
		ensureBuildPolicy(collection, repoData, branchName)
		// create other policies ...
		ensureMinimumApproversPolicy(collection, repoData, branchName)
		ensureLinkedWorkItemsPolicy(collection, repoData, branchName)
		ensureMergeStrategyPolicy(collection, repoData, branchName)
		ensureCommentResolutionPolicy(collection, repoData, branchName)
	}
	/**
	 *  This method creates and applies the CI build policy for validating code merges for new pull requests.
	 *  It also extends to create the CI and Release build definitions if they do not already exist.
	 *  
	 *  @return Response
	 */
	public def ensureBuildPolicy(def collection, def repoData, def branchName) {
		
		// See if branch participates in build policy enforcement
		String enforceBuildPolicy = this.branchProps.getProperty("enforce-build-policy")
		if (enforceBuildPolicy != null && !enforceBuildPolicy.equalsIgnoreCase("true")) {
			log.debug("PolicyManagementService::ensureBuildPolicy -- Branch opted OUT of build policy enforcement ...")
			return
		}
		
		// get the CI build
		def projectData = repoData.project
		// check for DR branch
		boolean isDRBranch = ("${branchName}".toLowerCase().startsWith("refs/heads/dr/"))
		def ciBuildTemplate = null
		def relBuildTemplate = null
		if (!isDRBranch) {
			ciBuildTemplate = this.branchProps.getProperty("build-template-ci")
			relBuildTemplate = this.branchProps.getProperty("build-template-release")
		}
		log.debug("PolicyManagementService::ensureBuildPolicy -- Specified CI build template = ${ciBuildTemplate}")
		log.debug("PolicyManagementService::ensureBuildPolicy -- Specified Release build template = ${relBuildTemplate}")

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
		def relDef = null
		def relDefName = ""
		if (relBuildId > -1) {
			def releaseTemplate = null
			// look for specified release template
			if (!isDRBranch) {
				releaseTemplate = this.branchProps.getProperty("release-template")
			}
			log.debug("PolicyManagementService::ensureBuildPolicy -- Release Build Definition created. Will attempt to create a release definition")
			relDef = releaseManagementService.ensureReleaseForBuild(collection, projectData, repoData, relBuildId, isDRBranch, releaseTemplate)
		}
		if (relDef == null) {
			log.error("PolicyManagementService::ensureBuildPolicy -- Release Definition creation failed!")
		} else {
			relDefName = "${relDef.name}"
			log.debug("PolicyManagementService::ensureBuildPolicy -- Release Definition created: "+relDefName)
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
				headers: [Accept: 'application/json;api-version=4.1;excludeUrls=true']
		)
		return result
	}

public def ensureMinimumApproversPolicy(def collection, def repoData, def branchName) {
	def projectData = repoData.project
	log.debug("PolicyManagementService::ensureMinimumApproversPolicy -- ")
	def policy = [id: -3, isBlocking: true, isDeleted: false, isEnabled: true, revision: 1,
	    type: [id: "fa4e907d-c16b-4a4c-9dfa-4906e5d171dd"],
	    settings:[minimumApproverCount: 1, creatorVoteCounts: false, allowDownvotes: false, resetOnSourcePush: true,
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

}

