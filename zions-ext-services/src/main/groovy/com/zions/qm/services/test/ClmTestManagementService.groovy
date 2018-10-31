package com.zions.qm.services.test

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.qm.services.rest.QmGenericRestClient

@Component
class ClmTestManagementService {
	
	@Autowired
	QmGenericRestClient qmGenericRestClient

	public ClmTestManagementService() {
		
	}
	
	def getTestItemsViaQuery(String query) {
		
	}

	public def nextPage(url) {
		def result = qmGenericRestClient.get(
			uri: url,
			headers: [Accept: 'text/xml'] );
		return result
	}

}
