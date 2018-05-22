package com.zions.vsts.services.admin.member;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient;
import groovy.json.JsonBuilder
import groovyx.net.http.ContentType

@Component
public class MemberManagementService {
	@Autowired(required=true)
	private GenericRestClient genericRestClient;
	
	@Autowired(required=true)
	private ProjectManagementService projectManagementService

	public MemberManagementService() {
		
	}
	
	public def addMember(String collection, String id,  def teams) {
		teams.each { team ->
			try {
				def tTeam = projectManagementService.ensureTeam(collection, team.project, team.team)
				if (tTeam != null) {
					addToTeam(collection, id, tTeam)
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
	private def addToTeam(String collection, String id, def team) {
		def req = [ 'aadGroups': "[]", 'exitingUsersJson': "[]", 'groupsToJoinJson': "[\"${team.id}\"]", 'newUsersJson': "[\"ZBC\\\\${id}\"]" ]
		def body = new JsonBuilder( req ).toString()
		def result = genericRestClient.post(
			requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${team.projectId}/${team.id}/_api/_identity/AddIdentities",
			query: ['__v': '5'],
			body: body,
			headers: [Accept: 'application/json'],
			)
	}
	
}
