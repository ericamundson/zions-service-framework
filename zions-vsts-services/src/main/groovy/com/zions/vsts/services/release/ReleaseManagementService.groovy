package com.zions.vsts.services.release;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.build.BuildManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.endpoint.EndpointManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient;
import groovy.json.JsonBuilder
import groovyx.net.http.ContentType

@Component
public class ReleaseManagementService {
	@Autowired
	private GenericRestClient genericRestClient
	
	@Autowired
	private ProjectManagementService projectManagementService
	
	@Autowired
	private CodeManagementService codeManagementService
	
	@Autowired
	private BuildManagementService buildManagementService
	
	@Autowired
	private EndpointManagementService endpointManagementService

	public ReleaseManagementService() {

	}
	
	public def ensureReleases(def collection, def project, def template, String xldEndpoint) {
		def projectData = projectManagementService.getProject(collection, project, true)
		def repos = codeManagementService.getRepos(collection, projectData)
		repos.value.each { repo ->
			ensureRelease(collection, projectData, repo,  template, xldEndpoint)
		}

	}
	
	public def ensureRelease(collection, projectData, repo, template, xldEndpoint) {
		def release = getRelease(collection, projectData, repo.name)
		if (release == null) {
			release = createRelease(collection, projectData, repo, template, xldEndpoint)
		}
	}
	
	public def createRelease(collection, project, repo, template, xldEndpoint) {
		def buildDef = buildManagementService.getBuild(collection, project, "${repo.name}-Release")
		if (buildDef == null) return null
		def endpoint = endpointManagementService.getServiceEndpoint(collection, project.id, xldEndpoint)
		if (endpoint == null) return null
		template.id = -1
		template.name = repo.name
		template.description = "Release definition for ${repo.name}"
		template.artifacts[0].alias = buildDef.name
		template.artifacts[0].definitionReference.artifactSourceDefinitionUrl.id = buildDef._links.web.href
		template.artifacts[0].definitionReference.definition.id = buildDef.id
		template.artifacts[0].definitionReference.definition.name = buildDef.name
		template.artifacts[0].definitionReference.project.id = project.id
		template.artifacts[0].definitionReference.project.name = project.name
		template.artifacts[0].definitionReference.sourceId = "${project.id}:${buildDef.id}"
		template.environments.each { env ->
			env.deployPhases.each { phase ->
				phase.workflowTasks.each { task ->
					if ("${task.taskId}" == '589dce45-4881-4410-bcf0-1afbd0fc0f65') {
						task.inputs.connectedServiceName = endpoint.id
						task.inputs.targetEnvironment = "Environments/${project.name}/${repo.name}/${env.name}"
					}
				}
			}
		}
		def body = new JsonBuilder(template).toPrettyString()
		
		def result = genericRestClient.post(
			requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/release/definitions",
			body: body,
			headers: [Accept: 'application/json;api-version=4.1-preview.3;excludeUrls=true'],
			)

	}
	
	public def getRelease(def collection, def project, String name) {
		def query = ['api-version':'4.1-preview.3','searchText':"${name}", isExactNameMatch: true]
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/release/definitions",
				query: query,
				)
		if (result == null || result.count == 0) return null
		query = ['api-version':'4.1-preview.3']
		def result1 = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_apis/release/definitions/${result.value[0].id}",
				query: query,
				)
		query = ['api-version':'4.1', 'propertyFilters':'processParameters']
		return result1
	}

}
