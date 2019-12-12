package com.zions.vsts.services.account

import com.zions.common.services.rest.IGenericRestClient
import groovyx.net.http.ContentType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AccountManagementService {
	
	@Autowired
	IGenericRestClient genericRestClient
	
	def getAccounts(String collection) {
		def result = genericRestClient.get(
				contentType: ContentType.JSON,
				uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/accounts",
				query: ['api-version': '5.1']
				)
		return result
	}
	
	def getAccount(String collection, String memberId) {
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${genericRestClient.getTfsUrl()}/${collection}/_apis/accounts",
			query: ['api-version': '5.1', memberId: memberId]
			)
		return result
	}
}
