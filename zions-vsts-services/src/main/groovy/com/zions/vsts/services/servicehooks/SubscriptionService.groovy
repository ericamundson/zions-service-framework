package com.zions.vsts.services.servicehooks
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.common.services.rest.IGenericRestClient
import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType

@Component
@Slf4j
class SubscriptionService {
	
	
	@Autowired
	IGenericRestClient genericRestClient
	
	def ensureSubscription(def projectInfo, def subscriptionData) {
		System.out.println("projectInfo.id = "+projectInfo.id)
		//System.out.println("projectId from publisherInputs = "+subscriptionData.publisherInputs.projectId)
		subscriptionData.publisherInputs.projectId = projectInfo.id
		def sub = getSubscription(projectInfo, subscriptionData)
		if (sub) {
			sub.consumerInputs = subscriptionData.consumerInputs
			return updateSubscription(sub)
		}
		return createSubscription(subscriptionData)
	}
	
	def getSubscription( def project, def subscriptionData) {
		def query = [consumerId: subscriptionData.consumerId, eventType: subscriptionData.eventType, publisherId: subscriptionData.publisherId, 'api-version': '5.1']
		def results = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/_apis/hooks/subscriptions",
			query: query
			)
		def retVal = null
		for (def sub in results.'value') {
			String projectIdSub = "${sub.publisherInputs.projectId}"
			String cProjectId = "${project.id}"
			// TODO: only checking project. probably need to verify other publisher inputs, ie. changedFields, workItemType, etc.
			if (projectIdSub == cProjectId) {
				if ("${sub.status}" != "disabledByUser") {
					def inputs = subscriptionData.publisherInputs
					Iterator<?> keys = inputs.keySet().iterator()
					boolean found = true
					while( keys.hasNext() ) {
						def key = keys.next()
						if ("${key}" == "projectId") continue
						System.out.println("SubscriptionService::getSubscription -- Comparing publisherInputs: key ${key}")
						def inputValue = inputs.get(key)
						def subValue = sub.publisherInputs.get(key)
						System.out.println("Input value: ${inputValue} ... subscription value: ${subValue}")
						if ( sub.publisherInputs.get(key) != inputs.get(key) ) {
							found = false
							break;
							//return null
						}
					}
					if (found) {
						System.out.println("SubscriptionService::getSubscription -- Found existing web hook subscription for ${subscriptionData.eventType}")
						log.info("SubscriptionService::getSubscription -- Found existing web hook subscription for ${subscriptionData.eventType} in project ${project.name}")
						return sub
					}
				}
			}
		}
		return retVal
	}
	
	def updateSubscription( def subscriptionData) {
		def results = genericRestClient.put(
			contentType: ContentType.JSON,
			requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/_apis/hooks/subscriptions",
			body: subscriptionData,
			query: ['api-version': '5.1']
		)
		return results
	}
	
	def createSubscription( def subscriptionData) {
		def results = genericRestClient.post(
			contentType: ContentType.JSON,
			requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/_apis/hooks/subscriptions",
			body: subscriptionData,
			query: ['api-version': '5.1']
		)
		return results
	}
}
