package com.zions.vsts.services.policy;

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import groovy.util.logging.Slf4j
import groovy.json.JsonBuilder;
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType

import com.zions.vsts.services.tfs.rest.GenericRestClient
import com.zions.vsts.services.build.BuildManagementService

/**
 * 
 * @author James McNabb
 *
 */
@Component
@Slf4j
public class PolicyManagementService {

	@Autowired
	private GenericRestClient genericRestClient

	@Autowired
	BuildManagementService buildManagementService

	public PolicyManagementService() {
	}

	/**
	 *  This method handles ensuring that all the necessary policies and build definitions are in
	 *  place for a new branch.
	 *  
	 *  @return Response - ??
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
		// first create the CI build policy
		ensureBuildPolicy(collection, repoData, branchName)
		// create other policies ...
		ensureMinimumApproversPolicy(collection, repoData, branchName)
		ensureLinkedWorkItemsPolicy(collection, repoData, branchName)
		ensureMergeStrategyPolicy(collection, repoData, branchName)
		ensureCommentResolutionPolicy(collection, repoData, branchName)
	}
	/**
	 *  This method creates and applies the CI build policy for validating code merges for new pull requests.
	 *  
	 *  @return Response - ??
	 */
	public def ensureBuildPolicy(def collection, def repoData, def branchName) {
		
		// get the CI build
		def projectData = repoData.project
		def ciBuild = buildManagementService.ensureBuildsForBranch(collection, projectData, repoData)
		log.debug("PolicyManagementService::ensureBuildPolicy -- ciBuild = "+ciBuild)
		def policy = [id: -2, isBlocking: true, isDeleted: false, isEnabled: true, revision: 1,
		    type: [id: "0609b952-1397-4640-95ec-e00a01b2c241"],
		    settings:[buildDefinitionId: ciBuild.id, displayName: "${repoData.name} validation", manualQueueOnly: false, queueOnSourceUpdateOnly:true, validDuration: 720,
				scope:[[matchKind: 'Exact',refName: branchName, repositoryId: repoData.id]]
			]
		]
		def res = createPolicy(collection, projectData, policy)
		log.debug("PolicyManagementService::ensureBuildPolicy -- result = "+res)
	}
	
	private def createPolicy(def collection, def projectData, def policy) {
		def body = new JsonBuilder(policy).toPrettyString()
		log.debug("PolicyManagementService::createPolicy -- Request body = "+body)
		def result = genericRestClient.post(
				requestContentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectData.name}/_apis/policy/configurations",
				body: body,
				headers: [Accept: 'application/json;api-version=4.0;excludeUrls=true']
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

