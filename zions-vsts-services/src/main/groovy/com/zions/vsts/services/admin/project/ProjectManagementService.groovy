package com.zions.vsts.services.admin.project

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.tfs.rest.GenericRestClient
import groovyx.net.http.ContentType

/**
 * Provides behaviors to manipulate and access project related objects.
 * 
 * @author z091182
 *
 */
@Component
class ProjectManagementService {
	@Autowired(required=true)
	private IGenericRestClient genericRestClient;
	
	private def projectDataCache = [:]

	public ProjectManagementService() {
		
	}
	
	/**
	 * Get the VSTS project information.
	 *  o UUID
	 *  o URL
	 *  o etc.
	 * @see <a href="file:../testdata/project.json">return data</a>
	 * @param collection
	 * @param name
	 * @return
	 */
	public def getProject(String collection, String name, boolean noUrl = false, def inquery = null) {
		def query = ['api-version':'5.1']
		if (inquery) {
			query = inquery
		}
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
	
	
	
	public def getProjects(String collection) {
		List projects = []
		int top = 100
		int count = 0
		int size = 100
		while (top == size) {
			if (count == 0) {
				def result = genericRestClient.get(
					contentType: ContentType.JSON,
					uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/projects",
					headers: [Accept: 'application/json'],
					query: ['api-version': '5.0', '$top' : top]
					)
				size = result.count
				projects.addAll(result.'value')
				count += size
			} else {
				def result = genericRestClient.get(
					contentType: ContentType.JSON,
					uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/projects",
					headers: [Accept: 'application/json'],
					query: ['api-version': '5.0', '$top' : top, continuationToken: "${count}"]
					)
				if (result == null) {
					top = 0
					continue;
				}
				size = result.count
				projects.addAll(result.'value')
				count += size
			}
		}
		return projects
	}
	
	def getProjectProperties(def collection, def project) {
		def projectData = getProject(collection, project)
		if (projectData == null) return null
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/projects/${projectData.id}/properties",
			query: ['api-version': '5.0-preview.1' ]
			)
		return result
	}
	
	def getProject(def collection, def projectId) {
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/projects/${projectId}",
			query: ['api-version': '6.0' ]
			)
		return result
	}
	
	def getProjectProperty(def collection, def project, def name) {
		// Get projectData from cache if available
		def projectData
		String key = "$collection-$project"
		if (projectDataCache.containsKey(key))
			projectData = projectDataCache[key]
		else {
			projectData = getProject(collection, project)
			projectDataCache.put(key, projectData)
		}
		if (projectData == null) return null
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
	
	def getTeams(String collection, String project ) {
		def query = ['api-version':'4.0']
		def eproject = URLEncoder.encode(project, 'UTF-8')
		eproject = eproject.replace('+', '%20')
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/projects/${eproject}/teams",
			headers: [Accept: 'application/json'],
			query: query
			)
		//if (result == null || !result.containsKey('id')) return null;
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
	
//	public def getAllProjects(String collection) {
//		def query = ['api-version':'4.0']
//		def result = genericRestClient.get(
//			contentType: ContentType.JSON,
//			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/projects",
//			headers: [Accept: 'application/json'],
//			query: query
//			)
//		return result
//
//	}
//	
//	public def getAllTeams(String collection) {
//		def projects = getAllProjects(collection)
//		def teams = [:]
//		def query = ['api-version':'4.0']
//		projects.value.each { project ->
//			def id = project.id
//			def result = genericRestClient.get(
//				contentType: ContentType.JSON,
//				uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/projects/${id}/teams",
//				headers: [Accept: 'application/json'],
//				query: query
//				)
//			result.value.each { team ->
//				teams["${project.name}:${team.name}"] = team
//				
//			}
//			
//		}
//		return teams
//	}

}
