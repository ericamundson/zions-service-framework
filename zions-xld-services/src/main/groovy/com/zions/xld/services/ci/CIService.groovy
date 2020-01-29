package com.zions.xld.services.ci

import com.zions.xld.services.rest.client.XldGenericRestClient
import groovyx.net.http.ContentType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CIService {
	
	@Autowired
	XldGenericRestClient xldGenericRestClient
	
	public def getCI(String path) {
		def result = xldGenericRestClient.get(
			contentType: ContentType.JSON,
			uri: "${xldGenericRestClient.xldUrl}/deployit/repository/ci/${path}"
		)
		return result

	}
}
