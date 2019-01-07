package com.zions.vsts.services.mr

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType

/**
 * Manages VSTS/Modern Requirements interaction to create SmartDocs.
 *
 * @author v070339
 *
 */
@Component
@Slf4j
class SmartDocManagementService {
	
	@Autowired(required=true)
	private IGenericRestClient genericRestClient
	
//	@Autowired(required=true)
//	private IGenericRestClient mrGenericRestClient
	
	@Autowired(required=true)
	ICacheManagementService cacheManagementService
	
	public SmartDocManagementService() {
		// TODO Auto-generated constructor stub
	}
	
	def createSmartDoc(def module, def collection, def tfsUser, def tfsCollectionGUID, def tfsProject, def tfsProjectURI, def tfsTeamGUID, def tfsOAuthToken, def mrTemplate, def mrFolder) {
		String body;
		String docTitle = module.getTitle()
		String domain = ""
		String userPassword = ""
		body = """
			{
			"userId": "$tfsUser",
			"userPassword":"$userPassword",
			"serverUrl":"${genericRestClient.getTfsUrl()}",
			"domain":"$domain",
			"oAuthAccessToken":"$tfsOAuthToken",
			"projectUri":"$tfsProjectURI",
			"projectName":"$tfsProject",
			"rootFolder":"$tfsProject",
			"isDefaultTeam": true,
			"teamGuid":"$tfsTeamGUID",
			"collectionName":"$collection",
			"collectionId":"$tfsCollectionGUID",
			"docName": "$docTitle",
			"templateName": "$mrTemplate",
			"folder": "$mrFolder",
			"autoRootCreation": true,
			"workItemDetails": ${getWorkitemDetails(module)}
			}
			"""
		return doPost(body)
		}
		
	private def doPost(def body) {
		/*
		def result = mrGenericRestClient.rateLimitPost(
			contentType: ContentType.JSON,
			uri: "${mrGenericRestClient.getMrUrl()}/Services/ExternalService.svc/api/smartdocs/create",
			body: body,
			headers: [accept: 'application/json']
			
			)
		if (result != null) {
			return result
		} else {
			log.error("SmartDoc request failed!")
			return null
		}
		*/
		return null
	}
	
	private def getWorkitemDetails(def module) {
		String jsonString = '['
		module.orderedArtifacts.each { artifact ->
			if (!artifact.isDeleted) {
				String id = "${artifact.getID()}-${artifact.getTfsWorkitemType()}"
				def cacheWI = cacheManagementService.getFromCache(id, ICacheManagementService.WI_DATA)
				if (cacheWI == null) {
					throw new FileNotFoundException(id)
				}
				if (jsonString[jsonString.size()-1] == '}') {
					jsonString = jsonString + ',' + '\n'
				}
				jsonString = jsonString + """{"id":"${cacheWI.id}","linkType":"Related"}"""
			}
		}
		return jsonString + ']'
	}
}