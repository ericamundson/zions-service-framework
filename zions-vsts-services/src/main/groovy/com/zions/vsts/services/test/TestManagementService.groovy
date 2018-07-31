package com.zions.vsts.services.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient;
import groovy.json.JsonBuilder
import groovyx.net.http.ContentType

@Component
public class TestManagementService {
	@Autowired(required=true)
	private GenericRestClient genericRestClient;
	
	@Autowired(required=true)
	private ProjectManagementService projectManagmentService;


	public TestManagementService() {
		
	}
	
	def getQueryHierarchy(def project) {
		def collection = ""
		def projectInfo = projectManagmentService.getProject(collection, project)
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectInfo.id}/_api/_TestQueries/GetQueryHierarchy",
			headers: ['Content-Type': 'application/json'],
			query: [itemTypes: 'ExploratorySession', itemTypes: 'TestResult', itemTypes: 'TestRun']
			)
		return result
	}
	
	def getTestRuns(def project) {
		def collection = ""
		def projectInfo = projectManagmentService.getProject(collection, project)
		//def queryHierarchy = getQueryHierarchy(project)
//		def query = new JsonBuilder( queryHierarchy.queries[0] ).toString()
//		
//		def queryJson = [queryJson: query]
//		def body = new JsonBuilder( queryJson ).toString()
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			//requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${projectInfo.id}/_apis/test/runs",
			headers: ['Content-Type': 'application/json'],
			query: ['api-version':'5.0-preview.2', includeRunDetails: true]
			)

		return result;

	}
}
