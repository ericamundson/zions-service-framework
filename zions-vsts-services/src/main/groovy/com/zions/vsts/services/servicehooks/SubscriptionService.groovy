package com.zions.vsts.services.servicehooks
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.common.services.rest.IGenericRestClient
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType

@Component
@Slf4j
class SubscriptionService {
	
	
	@Autowired
	IGenericRestClient genericRestClient
	
	def ensureSubscription(def projectInfo, def subscriptionData) {
		log.debug("projectInfo.id = "+projectInfo.id)
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
						log.debug("SubscriptionService::getSubscription -- Comparing publisherInputs: key ${key}")
						def inputValue = inputs.get(key)
						def subValue = sub.publisherInputs.get(key)
						log.debug("Input value: ${inputValue} ... subscription value: ${subValue}")
						if ( sub.publisherInputs.get(key) != inputs.get(key) ) {
							found = false
							break;
							//return null
						}
					}
					if (found) {
						//System.out.println("SubscriptionService::getSubscription -- Found existing web hook subscription for ${subscriptionData.eventType}")
						log.info("SubscriptionService::getSubscription -- Found existing web hook subscription for ${subscriptionData.eventType} in project ${project.name}")
						return sub
					}
				}
			}
		}
		return retVal
	}
	
	def getSubscriptionById( String subscriptionid) {
		def query = ['api-version': '6.1-preview.1']
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/_apis/hooks/subscriptions/${subscriptionid}",
			query: query
			)
		return result
	}

	def getWebhookSubscriptions() {
		//def query = [consumerId: subscriptionData.consumerId, eventType: subscriptionData.eventType, publisherId: subscriptionData.publisherId, 'api-version': '5.1']
		def query = [consumerActionId: 'httpRequest', 'api-version': '5.1']
		def results = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/_apis/hooks/subscriptions",
			query: query
			)
		return results
	}
	
	public def getProjectSubscriptions(collection) {
		//def collection = ""
		//def projectInfo = projectManagmentService.getProject(collection, project)
		
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			//requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/hooks/subscriptions",
			headers: ['Content-Type': 'application/json'],
			query: ['api-version':'6.0']
			)

		return result;

	}
	
	//hardcoded values are publisherId: tfs, resourceVersion: "1.0-preview.1", consumerId: "webHooks", "areaPath": "[Any]",
	public def replaceSubscription(collection, projectId, subId, url, eventType, userName, passWord) {
		
		def uri = "${genericRestClient.getTfsUrl()}/${collection}/_apis/hooks/subscriptions/${subId}?api-version=6.0&bypassRules=True&suppressNotifications=true"
		//def body = ['name': name, 'state': state, 'comment': comment, 'createdDate': createdDate, 'starteDate': startedDate, 'completedDate': completedDate, 'owner': [ 'displayName': owner], 'plan': [ 'id': testplanId], 'pointIds':   testpointIds ]
		def body = ['publisherId': 'tfs', 'eventType': eventType, 'resourceVersion': '1.0-preview.1', 'consumerId': 'webHooks', 'consumerActionId': 'httpRequest', 'publisherInputs': [ 'areaPath': '[Any]', 'workItemType': '', 'projectId': projectId], 'consumerInputs': [ 'url': url, 'basicAuthUsername': userName, 'basicAuthPassword': passWord]]
		
		String sbody = new JsonBuilder(body).toPrettyString()
		//put stop here json builder to prettystring look at what sbody looks like as formatted json
		//should have same format as body in successful talend execution
		def result = genericRestClient.put(
					  
			requestContentType: ContentType.JSON,
			contentType: ContentType.JSON,
			uri: uri,
			body: sbody,
			//headers: [Accept: 'application/json'],
			query: ['api-version': '5.1-preview.1' ]
			)
		return result
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

	def deleteSubscription( def subscriptionId) {
		def results = genericRestClient.delete(
			contentType: ContentType.JSON,
			requestContentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/_apis/hooks/subscriptions/${subscriptionId}",
			query: ['api-version': '6.0']
		)
		return results
	}

}
