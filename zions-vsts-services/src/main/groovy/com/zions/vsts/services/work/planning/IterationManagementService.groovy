package com.zions.vsts.services.work.planning

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.admin.project.ProjectManagementService
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType

@Component
class IterationManagementService {
	
	@Autowired
	private IGenericRestClient genericRestClient;
	
	@Autowired
	private ProjectManagementService projectManagementService;

	public IterationManagementService() {}
	
	def processIterationData(collection, project, iterations, parent, currentIterations) {
		iterations.each { iteration ->
			currentIterations = ensureIteration(collection, project, iteration, parent, currentIterations)
			processIterationData(collection, project, iteration.children, iteration, currentIterations)
		}
	}
	
	def ensureIteration(collection, project, iteration, parent, currentIterations) {
		def relatedArea = getRelated(iteration, currentIterations)
		if (relatedArea == null) {
			def relatedParent = getRelated(parent, currentIterations)
			def it = createIteration(collection, project, iteration, relatedParent)
			currentIterations = getIterationData(collection, project.name)
		}
		return currentIterations
	}
	def getRelated(iteration, currentIterations) {
		if (iteration == null) {
			return currentIterations.treeValues[0]
		}
		def related = null
		String[] iterationLoc = "${iteration.id}".split('/')
		def cIterations = currentIterations.treeValues[0]
		iterationLoc.each { node ->
			//boolean flag = false
			
			def found = null
			cIterations.children.each { child ->
				if ("${node}" == "${child.text}") {
					found = child
				}
			}
			if (found != null) {
				cIterations = found
			}
			related = found
		}
		return related
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

	def createIteration(def collection, def project, def iteration, def parent) {
		String sd = null
		if (iteration.startDate != null) {
			sd = "\\/Date(${iteration.startDate})\\/"
		}
		String ed = null
		if (iteration.endDate != null) {
			ed = "\\/Date(${iteration.endDate})\\/"
		}
		def operationData = [ 'NodeId': '00000000-0000-0000-0000-000000000000',  'NodeName': "${iteration.name}", 'IterationStartDate':sd, 'IterationEndDate': ed, 'ParentId': "${parent.id}" ]
		def opData = new JsonBuilder( operationData ).toString()
		def bodyData = ['operationData': opData, syncWorkItemTracking: false]
		def body = new JsonBuilder( bodyData ).toString()
		body =body.replace("\\\\", "\\")

		def eproject = URLEncoder.encode(project.name, 'UTF-8')
		eproject = eproject.replace('+', '%20')

		def result = genericRestClient.post(
			requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${project.id}/_admin/_Areas/CreateClassificationNode",
			query: ['__v': 5, useApiUrl: true],
			body: body,
			headers: [Accept: 'application/json, text/javascript, */*; q=0.01',
				Referer: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_admin/_Work"],
			)
		return result

	}
	

}
