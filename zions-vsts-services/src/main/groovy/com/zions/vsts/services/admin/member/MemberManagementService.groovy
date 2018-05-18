package com.zions.vsts.services.admin.member;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zions.vsts.services.tfs.rest.GenericRestClient;

import groovyx.net.http.ContentType

@Component
public class MemberManagementService {
	@Autowired(required=true)
	private GenericRestClient genericRestClient;

	public MemberManagementService() {
		
	}
	
	public def addMember(String collection, String id,  String[] teams) {
		def teamsData = getAllTeams(collection);
		return null;
	
	}
	
	public def getAllProjects(String collection) {
		def query = ['api-version':'4.0']
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/projects",
			headers: [Accept: 'application/json'],
			query: query
			)
		return result

	}
	
	public def getAllTeams(String collection) {
		def projects = getAllProjects(collection)
		def teams = [:]
		def query = ['api-version':'4.0']
		projects.value.each { project -> 
			def name = project.name
			def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/projects/${name}/teams",
				headers: [Accept: 'application/json'],
				query: query
				)
			result.value.each { team ->
				teams["${project.name}:${team.name}"] = team
				
			}
			
		}
		return teams
	}
}
