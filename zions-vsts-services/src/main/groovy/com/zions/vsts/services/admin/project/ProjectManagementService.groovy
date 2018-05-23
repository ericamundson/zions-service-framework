package com.zions.vsts.services.admin.project

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.vsts.services.tfs.rest.GenericRestClient
import groovyx.net.http.ContentType

@Component
class ProjectManagementService {
	@Autowired(required=true)
	private GenericRestClient genericRestClient;

	public ProjectManagementService() {
		
	}
	
	public def getProject(String collection, String name) {
		def query = ['api-version':'4.0']
		def eproject = URLEncoder.encode(name, 'UTF-8')
		eproject = eproject.replace('+', '%20')
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/projects/${eproject}",
			headers: [Accept: 'application/json'],
			query: query
			)
		return result

	}

	public def ensureProject(String collection, String name) {
		
	}
	
	private def getTeam(String collection, String project, String team ) {
		def query = ['api-version':'4.0']
		def eproject = URLEncoder.encode(project, 'UTF-8')
		eproject = eproject.replace('+', '%20')
		def eteam = URLEncoder.encode(team, 'UTF-8')
		eteam = eteam.replace('+', '%20')
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/projects/${eproject}/teams/${eteam}",
			headers: [Accept: 'application/json'],
			query: query
			)
		if (result == null || !result.containsKey('id')) return null;
		return result
	}
	
	public def ensureTeam(String collection, String project, String name) {
		def team = getTeam(collection, project, name) 
		if (team == null) {
			def query = ['api-version':'4.0']
			def eproject = URLEncoder.encode(project, 'UTF-8')
			eproject = eproject.replace('+', '%20')
			def result = genericRestClient.post(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/projects/${eproject}/teams",
				headers: [Accept: 'application/json'],
				query: query,
				body: ['name': name]
				)
			team = result
		}
		return team
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
			def id = project.id
			def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/projects/${id}/teams",
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
