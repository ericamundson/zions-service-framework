package com.zions.vsts.services.work.templates
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient

import groovy.json.JsonBuilder
import groovyx.net.http.ContentType
import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service;

@Component
public class ProcessTemplateService  {
	
	
	@Autowired(required=true)
	private GenericRestClient genericRestClient;
	
	@Autowired(required=true)
	private ProjectManagementService projectManagementService;

	
    public ProcessTemplateService() {
	}
	
	def getWorkItems(String collection, String project) {
		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def aproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/work/processes/${processTemplateId}/workitemtypes",
			headers: ['Content-Type': 'application/json'],
			query: ['api-version': '5.0-preview.2']
			)

		return result;

	}

	public def getWorkitemTemplate(String collection, String project,  String workItemName) {
		def projectData = projectManagementService.getProject(collection, project)
//		def processTemplateId = projectManagementService.getProjectProperty(collection, project, 'System.ProcessTemplateType')
		def aworkItemName = URLEncoder.encode(workItemName, 'utf-8').replace('+', '%20')
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectData.id}/_apis/wit/workitemtypes/${aworkItemName}/fields",
			headers: ['Content-Type': 'application/json'],
			query: ['api-version': '5.0-preview.3', '$expand': 'all']
			)

		return result;
	}
	
	def getWorkitemTemplateXML(String collection, String project,  String workItemName) {
		def xml = new StringBuilder(), serr = new StringBuilder()
		def proc = "witadmin exportwitd /collection:\"${genericRestClient.getTfsUrl()}/${collection}\" /p:\"${project}\" /n:\"${workItemName}\"".execute()
		proc.waitForProcessOutput(xml, serr)
		return xml
	}
	
	def getField(String url) {
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: url,
			query: ['$expand': 'all', 'api-version': '4.1'],
			headers: ['Content-Type': 'application/json']
			)
		return result
	}
	def getField(def collection, def project, def refName) {
		def projectData = projectManagementService.getProject(collection, project)
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectData.id}/_apis/wit/fields/${refName}",
			headers: ['Content-Type': 'application/json'],
			query: ['api-version': '5.0-preview.2', '$expand': 'all']
			)
		return result
	}

	public def updateWorkitemTemplate(String collection, String project,  String workItemName, String body) {
		def aproject = URLEncoder.encode(project).replace('+', '%20')
		def aworkItemName = URLEncoder.encode(workItemName).replace('+', '%20')
		def result = genericRestClient.put(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${aproject}/_apis/wit/workItemTypes/${aworkItemName}",
			body: body,
			headers: [Accept: 'application/json']
			)
		return result;
	}
	
	def updateWorkitemTemplates(def collection, def project, def mapping, def ccmWits) {
		
	}

}

