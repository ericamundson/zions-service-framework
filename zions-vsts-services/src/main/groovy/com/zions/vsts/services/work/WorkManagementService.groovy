package com.zions.vsts.services.work

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient
import groovy.json.JsonBuilder
import groovyx.net.http.ContentType

@Component
class WorkManagementService {
	
	@Autowired(required=true)
	private GenericRestClient genericRestClient;
	
	@Autowired(required=true)
	private ProjectManagementService projectManagementService;
	
	@Autowired(required=true)
	@Value('${cache.location}')
	private String cacheLocation

	public WorkManagementService() {
		
	}
	
	def batchWIChanges(def collection, def project, def witData) {
		def body = new JsonBuilder(witData).toPrettyString()
		//		File s = new File('defaultwit.json')
		//		def w = s.newDataOutputStream()
		//		w << body
		//		w.close()
		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/wit/\$batch",
			body: body,
			headers: [accept: 'application/json'],
			query: ['api-version': '4.1']
			
			)
		cacheResult(result)
	}
	
	def cacheResult(result) {
		
	}
	
	def testBatchWICreate(def collection, def project) {
		def projectData = projectManagementService.getProject(collection, project)
		def wiBatch = [ 
			]
		
		def request1 = [method:'PATCH', uri: "/${projectData.id}/_apis/wit/workitems/\$Task?api-version=5.0-preview.3", headers: ['Content-Type': 'application/json-patch+json'], body: []]
		def rBody1 = [op:'add', path: '/fields/System.Title', from: null, value:'This is a test']
		request1.body.add(rBody1)
		wiBatch.add(request1)
		def body = new JsonBuilder(wiBatch).toPrettyString()
		//		File s = new File('defaultwit.json')
		//		def w = s.newDataOutputStream()
		//		w << body
		//		w.close()
		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/wit/\$batch",
			body: body,
			headers: [accept: 'application/json'],
			query: ['api-version': '4.1']
			
			)

	}

}
