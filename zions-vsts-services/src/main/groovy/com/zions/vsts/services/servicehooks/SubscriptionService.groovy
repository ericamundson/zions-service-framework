package com.zions.vsts.services.servicehooks
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.common.services.rest.IGenericRestClient
import groovyx.net.http.ContentType

@Component
class SubscriptionService {
	
	
	@Autowired
	IGenericRestClient genericRestClient
	
	def ensureSubscription(def projectInfo, def subscriptionData) {
		//def projectInfo = projectManagementService.getProject('', project)
		subscriptionData.publisherInputs.projectId = projectInfo.id
		def sub = getSubscription(projectInfo, subscriptionData)
		if (sub) {
			//sub = updateSubscription(projectInfo, subscriptionData)
			return sub
		}
		return createSubscription(projectInfo, subscriptionData)
	}
	
	def getSubscription( def project, def subscriptionData) {
		def query = [consumerId: subscriptionData.consumerId, eventType: subscriptionData.eventType, publisherId: subscriptionData.publisherId, 'api-version': '5.1']
		def results = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/_apis/hooks/subscriptions",
			query: query
			)
		def retVal = null	
		results.'value'.each { sub ->
			String projectIdSub = "${sub.publisherInputs.projectId}"
			String cProjectId = "${project.id}"
			if (projectIdSub == cProjectId) {
				retVal = sub
				return
			}
		}
		return retVal
	}
	
	def updateSubscription( def project, def subscriptionData) {
		
	}
	
	def createSubscription( def project, def subscriptionData) {
		def results = genericRestClient.post(
			contentType: ContentType.JSON,
			requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/_apis/hooks/subscriptions",
			body: subscriptionData,
			query: ['api-version': '5.1']
			)

	}
}
