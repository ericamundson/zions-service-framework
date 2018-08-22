package com.zions.vsts.services.work.planning

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.admin.project.ProjectManagementService
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType

@Component
class IterationManagmentService {
	
	@Autowired
	private IGenericRestClient genericRestClient;
	
	@Autowired
	private ProjectManagementService projectManagementService;

	public IterationManagmentService() {}
	
	def processIterationStructure(collection, project, structure) {
		
	}
	
	def getIterationData(def collection, def project) {
		def eproject = URLEncoder.encode(project, 'UTF-8')
		eproject = eproject.replace('+', '%20')
		def result = genericRestClient.get(
			contentType: ContentType.HTML,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_admin/_Work",
			//headers: headers,
			query: ['_a':'areas']
			)
		def dataNode = result.'**'.find { node ->
			if ("${node.name()}".toLowerCase() == 'div' && "${node.@class}".toLowerCase() == 'project-admin-work') {
				return true
			}
			return false
		}
		def iterations = null
		if (dataNode != null) {
			def json = dataNode.SCRIPT.text()
			JsonSlurper s = new JsonSlurper()
			def config = s.parseText(json)
			iterations = config.iterations
		}
		return iterations
	}

	def createIteration(collection, project, parentId, name, startDate, endDate) {
		
	}
	

}
