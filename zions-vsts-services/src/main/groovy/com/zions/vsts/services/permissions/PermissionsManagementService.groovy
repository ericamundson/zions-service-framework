package com.zions.vsts.services.permissions

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.endpoint.EndpointManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType

@Component
class PermissionsManagementService {
	@Autowired
	private GenericRestClient genericRestClient
	
	@Autowired
	private CodeManagementService codeManagementService
	
	@Autowired
	private ProjectManagementService projectManagementService
	
	@Autowired
	private MemberManagementService memberManagementService


	public PermissionsManagementService() {
		
	}
	
	public def ensureTeamToRepo(String collection, String project, String repo, String teamName, String templateName) {
		def permissionsTemplate = getResource(templateName)
		def projectData = projectManagementService.getProject(collection, project)
		def team = memberManagementService.getTeam(collection, projectData, teamName)
		def repoData = codeManagementService.getRepo(collection, projectData, repo)
		
		//def perms = getRepoPermissions(collection, projectData, repoData, team )
		if (!hasIdentity(collection, projectData, repoData, team)) {
			def identity = addIdentityForPermissions(collection, projectData, repoData, team)
			def perms = getRepoPermissions(collection, projectData, repoData, team.localId )
			manageRepoPermission(collection, projectData, repoData, permissionsTemplate, perms)
		}
	}
	
	def updateBuilderPermissions(String collection, String project, String template) {
		def permissionsTemplate = getResource(template)
		def projectData = projectManagementService.getProject(collection, project)
		def repos = codeManagementService.getRepos(collection, projectData)
		repos.value.each { repoData ->
			def builder = getRepoIdentity(collection, projectData, repoData, 'Project Collection Build Service (DefaultCollection)')
			def perms = getRepoPermissions(collection, projectData, repoData, builder.TeamFoundationId )
			manageRepoPermission(collection, projectData, repoData, permissionsTemplate, perms)
		}
	}
	
	def addIdentityForPermissions(collection, projectData, repoData, team) {
		def req = [ 'existingUsersJson': "[\"${team.localId}\"]",  'newUsersJson': "[]" ]
		def body = new JsonBuilder( req ).toString()
		def eproject = URLEncoder.encode(projectData.name, 'UTF-8')
		eproject = eproject.replace('+', '%20')

		def result = genericRestClient.post(
			requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectData.id}/_api/_security/AddIdentityForPermissions",
			query: ['__v': 5, repositoryId: "${repoData.id}"],
			body: body,
			headers: [Accept: 'application/json, text/javascript, */*; q=0.01',
				Referer: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_admin/_versioncontrol?_a=security&repositoryId=${repoData.id}"],
			)
		return result
	}
	
	def manageRepoPermission(collection, projectData, repoData, permTemplate, perms) {
		def updates = permTemplate.updates
		updates.each { perm ->
			perm.Token = "repoV2/${projectData.id}/${repoData.id}/"
			//perm.NamespaceId = "${team.localId}"
		}
		def permData = [IsRemovingIdentity: false, 
			TeamFoundationId: perms.currentTeamFoundationId, 
			DescriptorIdentityType: perms.descriptorIdentityType, 
			DescriptorIdentifier: perms.descriptorIdentifier,
			PermissionSetId: '2e9eb7ed-3c0a-47d4-87c1-0ffdd275fd87',
			PermissionSetToken: "repoV2/${projectData.id}/${repoData.id}/",
			RefreshIdentities: false,
			Updates: updates,
			TokenDisplayName: null]
		def oupdates = new JsonBuilder( permData ).toString()
		def updatePackage = [updatePackage: "${oupdates}"]
		def body = new JsonBuilder( updatePackage ).toString()
		def result = genericRestClient.post(
			requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectData.id}/_api/_security/ManagePermissions",
			query: ['__v': '5'],
			body: body,
			headers: [Accept: 'application/json, text/javascript, */*; q=0.01'],
			)

	}
	
	public boolean hasIdentity(collection, project, repo, team) {
		def identities = getCurrentIdentities(collection, project, repo)
		boolean hasId = false;
		identities.identities.each { id -> 
			if ("${team.displayName}" == "${id.DisplayName}") {
				hasId = true
				return
			}
		}
		return hasId
	}
	
	public def getCurrentIdentities(collection, project, repo) {
		def query = [__v: 5, permissionSetId: '2e9eb7ed-3c0a-47d4-87c1-0ffdd275fd87', permissionSetToken: "repoV2/${project.id}/${repo.id}"]
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_api/_security/ReadExplicitIdentitiesJson",
			query: query,
			)
		return result

	}
	
	def getRepoIdentity(collection, project, repo, name) {
		def identities = getCurrentIdentities(collection, project, repo)
		def retVal = null;
		identities.identities.each { id ->
			if ("${name}" == "${id.DisplayName}") {
				retVal = id
				return
			}
		}
		return retVal

	}
	
	public def getRepoPermissions(String collection, def project, def repo, def tfid) {
		if (tfid == null) return
		def query = [__v: 5, tfid: tfid, permissionSetId: '2e9eb7ed-3c0a-47d4-87c1-0ffdd275fd87', permissionSetToken: "repoV2/${project.id}/${repo.id}"]
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_api/_security/DisplayPermissions",
			query: query,
			)
		return result

	}
	
	public def getResource(String name) {
		def template = null
		try {
		def s = getClass().getResourceAsStream("/grant_templates/${name}.json")
		JsonSlurper js = new JsonSlurper()
		template = js.parse(s)
		} catch (e) {}
		return template
	}

}
