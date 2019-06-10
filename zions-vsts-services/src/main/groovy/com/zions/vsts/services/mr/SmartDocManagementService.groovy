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
import java.time.format.DateTimeFormatter

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
		// Do nothing
	}
	
	def createSmartDoc(def module, def tfsUrl, def collection, def tfsCollectionGUID, def tfsProject, def tfsProjectURI, def tfsTeamGUID, def tfsAltUser, def tfsAltPassword, def mrTemplate, def mrFolder) {
		def date = new Date()
		String body;
		String docTitle = "${module.getTitle()}-${date.format('yyyyMMddHHmmss')}"
		String domain = ""
		String userPassword = ""
		def index = 0
		String wiDetails = """[{"id":"${getVstsID(module)}","linkType":"","links":${getWorkitemDetails(0, module).detailString}}]"""
		body = """
			{
			"userId": "$tfsAltUser",
			"userPassword":"$tfsAltPassword",
			"serverUrl":"$tfsUrl/$collection",
			"domain":"$domain",
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
			"autoRootCreation": false,
			"workItemDetails": ${wiDetails}
			}
			"""
			println(body)
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
	}
	
	private WorkItemDetails getWorkitemDetails(def iStart, def module) {
		String jsonString = '['
		def i = iStart
		def iStartDepth = module.orderedArtifacts[iStart].getDepth()
		while(i < module.orderedArtifacts.size() && module.orderedArtifacts[i].getDepth() >= iStartDepth) {
			def artifact = module.orderedArtifacts[i]
			if (!artifact.isDeleted) {
				if (jsonString[jsonString.size()-1] == '}') {
					jsonString = jsonString + ',' + '\n'
				}
				if (i < module.orderedArtifacts.size() - 1 && module.orderedArtifacts[i+1].getDepth() > module.orderedArtifacts[i].getDepth()) {
					def wiDetails = getWorkitemDetails(i+1, module)
					jsonString = jsonString + """{"id":"${getVstsID(artifact)}","linkType":"Related","links":${wiDetails.detailString}}"""
					i = wiDetails.index	
				}
				else {
					jsonString = jsonString + """{"id":"${getVstsID(artifact)}","linkType":"Related"}"""
					i++
				}
			}
			else {
				i++
			}
		}
		

		return new WorkItemDetails(jsonString + ']',i)
	}
	
	private String getVstsID(def artifact) {
		String id = artifact.getCacheID()
		def cacheWI = cacheManagementService.getFromCache(id, ICacheManagementService.WI_DATA)
		if (cacheWI == null) {
			throw new FileNotFoundException(id)
		}
		return cacheWI.id
	}
}