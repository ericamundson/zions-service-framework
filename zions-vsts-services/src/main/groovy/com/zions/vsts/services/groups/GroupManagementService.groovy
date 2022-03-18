package com.zions.vsts.services.groups

import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.security.SecurityManagementService

import groovy.json.JsonBuilder
import groovy.util.logging.Log
import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
@Slf4j
class GroupManagementService {
	
	@Autowired
	IGenericRestClient genericRestClient
	
	@Autowired
	SecurityManagementService securityManagementService

	def getOrgGroups(String collection) {
		def orgGroups = []
		String org = "$collection".toLowerCase()
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl().replace('//dev.', '//vssps.dev.')}/${collection}/_apis/graph/groups",
				query: ['api-version': '6.0-preview.1'],
				withHeader: true
			)
			
		while (true) {
			result.data.value.each { group ->
				String groupOrg = "[${group.principalName}]".toLowerCase().substring(2,org.length()+2)
				if (groupOrg == org)
					orgGroups.add(group)
			}
			if (result.headers.'X-MS-ContinuationToken') {
				result = genericRestClient.get(
					contentType: ContentType.JSON,
					uri: "${genericRestClient.getTfsUrl().replace('//dev.', '//vssps.dev.')}/${collection}/_apis/graph/groups",
					query: ['api-version': '6.0-preview.1', continuationToken: result.headers.'X-MS-ContinuationToken'],
					withHeader: true
					)
			} else {
				break
			}
		}
		return orgGroups
	}
	
	def createOrgGroup(String collection, def name, def description) {
		log.info("Creating group $name")
		def groupData = [displayName: name, description: description]
		def body = new JsonBuilder(groupData).toPrettyString()

		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl().replace('//dev.', '//vssps.dev.')}/${collection}/_apis/graph/groups",
			headers: [accept: 'application/json'],
			query: ['api-version': '7.1-preview.1'],
			body: body
			)
		return result
	}
	
	def updateGroup(String collection, def name, def descriptor, def description) {
		log.info("Updating group $name")
		def groupData = []
		groupData.add([op: "replace", path: "/description", from: null, value: description])
		def body = new JsonBuilder(groupData).toPrettyString()

		def result = genericRestClient.patch(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl().replace('//dev.', '//vssps.dev.')}/${collection}/_apis/graph/groups/$descriptor",
			headers: ['Content-Type': 'application/json-patch+json', accept: 'application/json'],
			query: ['api-version': '7.1-preview.1'],
			body: body
			)
		return result
	}
	
	def getGroupAcls(String collection, def descriptor) {
		// Get Graph security namespace
		def namespaces = securityManagementService.getNamespaces(collection)
		namespaces.each { ns ->
			def acls = securityManagementService.queryAcls(collection, ns.namespaceId, toIdentityDescriptor(descriptor))
			def i = 1
		}
		
		/*
		if (ns)
		def acls = securityManagementService.queryAcls(collection, ns.namespaceId, toIdentityDescriptor(descriptor))
		*/
	}
	private def toIdentityDescriptor(String descriptor) {
		def parts = "$descriptor".toString().split('\\.')
  
		return "Microsoft.TeamFoundation.Identity;${base64Decode(parts[1])}";
	}
  
	private String base64Decode(String base64EncodedData) {
		def lengthMod4 = base64EncodedData.length() % 4
  
		if (lengthMod4 != 0) //fix Invalid length for a Base-64 char array or string
			base64EncodedData += fillString('=', 4 - lengthMod4)
  
		return Base64.getEncoder().encodeToString(base64EncodedData.getBytes())
  	}
	private static String fillString(def ch, def stringLength){
		  
        //create new string from char array of required size
        String str = new String(new char[stringLength])
        
        //replace all NUL chars '\0' with specified char
        return str.replace('\0', ch)

	  }
}
