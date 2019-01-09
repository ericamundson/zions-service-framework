package com.zions.vsts.services.mr

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.admin.project.ProjectManagementService
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
	class WorkItemDetails {
		WorkItemDetails(String details, Integer i) {
			detailString = details
			index = i
		}
		String detailString
		Integer index
	}

	@Autowired(required=true)
	private IGenericRestClient mrGenericRestClient
	
	@Autowired(required=true)
	ICacheManagementService cacheManagementService
	
	public SmartDocManagementService() {
		// TODO Auto-generated constructor stub
	}
	
	def createSmartDoc(def module, def collection, def mrTfsUrl, def tfsCollectionGUID, def tfsProject, def tfsProjectURI, def tfsTeamGUID, def tfsOAuthToken, def mrTemplate, def mrFolder) {
		String body;
		String docTitle = module.getTitle()
		String domain = ""
		String userPassword = ""
		def index = 0
		def wiDetails = getWorkitemDetails(0, module)
		body = """
			{
			"userId": "${mrGenericRestClient.getUserid()}",
			"userPassword":"$userPassword",
			"serverUrl":"$mrTfsUrl",
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
			"workItemDetails": ${wiDetails.detailString}
			}
			"""

			return doPost(body)
		}
		
	private def doPost(def body) {
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

		return null
	}
	
	private WorkItemDetails getWorkitemDetails(def iStart, def module) {
		String jsonString = '['
		def i = iStart
		def iStartDepth = module.orderedArtifacts[iStart].getDepth()
		while(i < module.orderedArtifacts.size() - 1 && module.orderedArtifacts[i].getDepth() >= iStartDepth) {
			def artifact = module.orderedArtifacts[i]
			if (!artifact.isDeleted) {
				String id = "${artifact.getID()}-${artifact.getTfsWorkitemType()}"
				def cacheWI = cacheManagementService.getFromCache(id, ICacheManagementService.WI_DATA)
				if (cacheWI == null) {
					throw new FileNotFoundException(id)
				}
				if (jsonString[jsonString.size()-1] == '}') {
					jsonString = jsonString + ',' + '\n'
				}
				if (module.orderedArtifacts[i+1].getDepth() > module.orderedArtifacts[i].getDepth()) {
					def wiDetails = getWorkitemDetails(i+1, module)
					jsonString = jsonString + """{"id":"${cacheWI.id}","linkType":"Related","links":${wiDetails.detailString}}"""
					i = wiDetails.index	
				}
				else {
					jsonString = jsonString + """{"id":"${cacheWI.id}","linkType":"Related"}"""
					i++
				}
			}
			else {
				i++
			}
		}
		return new WorkItemDetails(jsonString + ']',i)
	}
}