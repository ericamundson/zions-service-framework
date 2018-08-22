package com.zions.vsts.services.admin.project

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.tfs.rest.GenericRestClient
import groovyx.net.http.ContentType

@Component
class ProjectManagementService {
	@Autowired(required=true)
	private IGenericRestClient genericRestClient;

	public ProjectManagementService() {
		
	}
	
	public def getProject(String collection, String name, boolean noUrl = false) {
		def query = ['api-version':'4.0']
		def headers = [Accept: 'application/json']
		if (noUrl) {
			headers.Accept = 'application/json;excludeUrls=true'
		}
		def eproject = URLEncoder.encode(name, 'UTF-8')
		eproject = eproject.replace('+', '%20')
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/projects/${eproject}",
			headers: headers,
			query: query
			)
		return result

	}
	
	def getProjectProperties(def collection, def project) {
		def projectData = getProject(collection, project)
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/projects/${projectData.id}/properties",
			query: ['api-version': '5.0-preview.1' ]
			)
		return result
	}
	
	def getProjectProperty(def collection, def project, def name) {
		def projectData = getProject(collection, project)
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/projects/${projectData.id}/properties",
			query: ['api-version': '5.0-preview.1' ]
			)
		def val = null
		result.value.each { item ->
			if ("${item.name}" == "${name}") {
				val = "${item.value}"
			}
		}
		return val
	}

	public def ensureProject(String collection, String name) {
		
	}
	
	def getTeam(String collection, String project, String team ) {
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
