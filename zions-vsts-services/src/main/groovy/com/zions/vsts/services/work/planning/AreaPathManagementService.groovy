package com.zions.vsts.services.work.planning

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType

class AreaPathManagementService {
	@Autowired
	private IGenericRestClient genericRestClient;

	def createAreaPath(def collection, def projectName, def areaPathBase, def areaPathNode) {

		def eproject = URLEncoder.encode(projectName, 'UTF-8')
		eproject = eproject.replace('+', '%20')

		def bodyData = ['name': areaPathNode]
		def body = new JsonBuilder( bodyData ).toString()

		def result = genericRestClient.post(
			requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/${eproject}/_apis/wit/classificationnodes/Areas/${areaPathBase}".replace('\\','/'),
			query: ['api-version': '5.1'],
			body: body,
			headers: [Accept: 'application/json'])
		return result

	}
}
