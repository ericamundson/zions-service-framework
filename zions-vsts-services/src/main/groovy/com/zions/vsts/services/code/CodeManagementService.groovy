package com.zions.vsts.services.code

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.endpoint.EndpointManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient
import groovy.json.JsonBuilder
import groovyx.net.http.ContentType

@Component
class CodeManagementService {
	@Autowired
	private GenericRestClient genericRestClient
	
	@Autowired 
	private ProjectManagementService projectManagementService
	
	@Autowired
	private EndpointManagementService endpointManagementService

	public CodeManagementService() {
		
	}
	
	public def ensureRepo(String collection, def project, String repoName) {
		def repo = getRepo(collection, project, repoName)
		if (repo == null) {
			repo = createRepo(collection, project, repoName)
		}
		return repo
	}
	
	public def createRepo(String collection, def project, String repoName) {
		def query = ['api-version':'4.1']
		def reqObj = [name: repoName, project: [id: project.id, name: project.name]]
		def body = new JsonBuilder(reqObj).toString()
		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			requestContentType: 'application/json',
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/git/repositories",
			query: query,
			body: body
			)
		return result

	}
	public def getRepo(String collection, def project, String repoName) {
		def query = ['api-version':'4.1']
		def repoNameE = URLEncoder.encode(repoName, 'UTF-8')
		repoNameE= repoNameE.replace('+', '%20')
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/git/repositories/${repoNameE}",
			query: query,
			)
		return result

	}

	public def importRepo(String collection, String project, String repoName, String importUrl, String bbUser, String bbPassword) {
		def projectData = projectManagementService.getProject(collection, project)
		def repo = ensureRepo(collection, projectData, repoName)
		def endpoint = endpointManagementService.createServiceEndpoint(collection, projectData.id, importUrl, bbUser, bbPassword)
		def query = ['api-version':'4.1-preview.1']
		def reqObj = [parameters: [deleteServiceEndpointAfterImportIsDone: true, gitSource: [url: importUrl, overwrite: false], serviceEndpointId: endpoint.id, tfvcSource: null]]
		def body = new JsonBuilder(reqObj).toString()
		def repoNameE = URLEncoder.encode(repoName, 'UTF-8')
		repoNameE = repoNameE.replace('+', '%20')
		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			requestContentType: 'application/json',
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectData.id}/_apis/git/repositories/${repoNameE}/importRequests",
			query: query,
			body: body
			)
		return result

	}
}
