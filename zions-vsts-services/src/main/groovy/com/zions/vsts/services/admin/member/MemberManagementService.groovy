package com.zions.vsts.services.admin.member;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient;
import groovy.json.JsonBuilder
import groovyx.net.http.ContentType

/**
 * Provides behaviors to manage users on a VSTS project.  Adding users to projects and teams.
 * @author z091182
 *
 */
@Component
public class MemberManagementService {
	/**
	 * Main interface to handle requests to VSTS collection.
	 */
	@Autowired(required=true)
	private IGenericRestClient genericRestClient;
	
	/**
	 * Access to project related behaviors
	 */
	@Autowired(required=true)
	private ProjectManagementService projectManagementService

	/**
	 * Default constructor
	 */
	public MemberManagementService() {
		
	}
	
	/**
	 * Adds members/user to team.
	 * 
	 * @param collection
	 * @param id
	 * @param teams
	 * @return
	 */
	public def addMemberToTeams(String collection, String email,  def teams) {
		teams.each { team ->
			try {
				def tTeam = projectManagementService.ensureTeam(collection, team.project, team.team)
				if (tTeam != null) {
					addToTeam(collection, email, tTeam)
				}
				
			} catch (e) {}
		}
		return null;
	
	}
	
	/**
	 * Accessing TFS private rest API to perform adding a member to a team.
	 * 
	 * @param collection
	 * @param id
	 * @param team
	 * @return
	 */
	private def addToTeam(String collection, String email, def team) {
		//ensureEntitlement(collection, email)
		def req = [ 'aadGroups': "[]", 'exitingUsersJson': "[]", 'groupsToJoinJson': "[\"${team.id}\"]", 'newUsersJson': "[\"${email}\"]" ]
		def body = new JsonBuilder( req ).toString()
		def result = genericRestClient.post(
			requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${team.projectId}/${team.id}/_api/_identity/AddIdentities",
			query: ['__v': '5'],
			body: body,
			headers: [Accept: 'application/json'],
			)
	}
	
	
	/**
	 * Get member from VSTS
	 * 
	 * @param collection
	 * @param signin
	 * @return
	 */
//	public def getMember(def collection, def signin) {
//		def req = [ 'filterByAncestorEntityIds': [], 
//			filterByEntityIds:[], 
//			identityTypes: ['user', 'group'], 
//			operationScopes: ['ims'],
//			properties: ['displayName', 'id', 'imageUrl', 'uniqueName', 'url', '_links'],
//			options: [MinResults:40, MaxResults: 40],
////			queryTypeHint: 'uid',
//			query: signin  ]
//		def body = new JsonBuilder( req ).toString()
//		def result = genericRestClient.post(
//			requestContentType: ContentType.JSON,
//			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/IdentityPicker/Identities",
//			body: body,
//			headers: [accept: 'application/json;api-version5.0-preview.1;excludeUrls=true'],
//			)
//		return result
//	}
	
	/**
	 * Return a Map of team members with key being the uniqueName of member.
	 * 
	 * @param collection
	 * @param project
	 * @param teamName
	 * @return
	 */
//	def getTeamMembersMap(collection, project, teamName) {
//		def projectData = projectManagementService.getProject(collection, project)
//		def teamData = getTeam(collection, projectData, teamName)
//		def result = genericRestClient.get(
//				contentType: ContentType.JSON,
//				uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/projects/${projectData.id}/teams/${teamData.id}/members",
//				query: ['api-version': '5.0-preview.2']
//				)
//		def retVal = [:]
//		result.value.each { ridentity ->
//			def identity = ridentity.identity
//			String uid = "${identity.uniqueName}"
//			retVal[uid.toLowerCase()] = identity
//		}
//		return retVal
//	}
	
	/**
	 * Return a Map of team members with key being the uniqueName of member.
	 *
	 * @param collection
	 * @param project
	 * @param teamName
	 * @return
	 */
	def getProjectMembersMap(collection, project) {
		def projectData = projectManagementService.getProject(collection, project)
		def teams = getAllTeams(collection, projectData)
		def retVal = [:]
		teams.value.each { teamData -> 
			def result = genericRestClient.get(
					contentType: ContentType.JSON,
					uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/projects/${projectData.id}/teams/${teamData.id}/members",
					query: ['api-version': '5.0-preview.2']
					)
			result.value.each { ridentity ->
				def identity = ridentity.identity
				String uid = "${identity.uniqueName}"
				if (retVal[uid.toLowerCase()] == null) {
					retVal[uid.toLowerCase()] = identity
				}
			}
		}
		return retVal
	}

//	public def getMemberAlt(def collection, def signin) {
//		def req = [ 'filterByAncestorEntityIds': [],
//			filterByEntityIds:[],
//			identityTypes: ['user', 'group'],
//			operationScopes: ['ims'],
//			properties: ["DisplayName", "IsMru", "ScopeName", "SamAccountName", "Active", "SubjectDescriptor", "Department", "JobTitle", "Mail", "MailNickname", "PhysicalDeliveryOfficeName", "SignInAddress", "Surname", "Guest", "TelephoneNumber", "Description"],
//			//: ['displayName', 'id', 'imageUrl', 'uniqueName', 'url', '_links'],
//			options: [MinResults:40, MaxResults: 40],
////			queryTypeHint: 'uid',
//			query: signin  ]
//		def body = new JsonBuilder( req ).toString()
//		def result = genericRestClient.post(
//			requestContentType: ContentType.JSON,
//			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/IdentityPicker/Identities",
//			body: body,
//			headers: [accept: 'application/json;api-version5.0-preview.1;excludeUrls=true'],
//			)
//		return result
//	}

	
	def getTeam(collection, project, teamName) {
		def eteam = URLEncoder.encode(teamName, 'utf-8')
		eteam = eteam.replace('+', '%20')
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/projects/${project.id}/teams/${eteam}",
				query: ['api-version': '5.0-preview.2']
				)
		return result
	}
	
	def getAllTeams(def collection, def project) {
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/projects/${project.id}/teams",
				query: ['api-version': '5.0-preview.2']
				)
		return result

	}

	/**
	 * Query for a specific team from VSTS using IdentityPicker interaction.
	 * 
	 * @param collection
	 * @param project
	 * @param team
	 * @return
	 */
	public def queryForTeam(def collection, def project, def team) {
		String cVal = collection
		if (cVal.size() == 0) {
			cVal = "${genericRestClient.tfsUrl}"
			cVal = cVal.substring("https://".size())
			cVal = cVal.substring(0, cVal.indexOf('.'))
		}
		def req = [ 'filterByAncestorEntityIds': [], 
			filterByEntityIds:[], 
			identityTypes: ['user', 'group'], 
			operationScopes: ['ims','ad', 'wmd'],
			properties: ['DisplayName', 'IsMru', 'ScopeName', 'SamAccountName', 'Active', 'SubjectDescriptor', 'Department', 'JobTitle', 'Mail', 'MailNickname', 'PhysicalDeliveryOfficeName'],
			filterByAncestorEntityIds: [],
			filterByEntityIds: [],
			options: [MinResults:40, MaxResults: 40, constraints: [], ExtensionId: 'F12CA7AD-00EE-424F-B6D7-9123A60F424F', CollectionScopeName: "${cVal}", ProjectScopeName: "${project.name}"],
			query: team  ]
		def body = new JsonBuilder( req ).toString()
		def result = genericRestClient.post(
			requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/IdentityPicker/Identities",
			body: body,
			headers: [Accept: 'application/json;api-version=5.0-preview.1;excludeUrls=true'],
			)
		return result
	}
}
