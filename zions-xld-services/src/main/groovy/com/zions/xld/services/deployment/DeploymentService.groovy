package com.zions.xld.services.deployment

import com.zions.xld.services.rest.client.XldGenericRestClient
import groovyx.net.http.ContentType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DeploymentService {
	
	@Autowired
	XldGenericRestClient xldGenericRestClient
	
	public boolean hasDeployment(String appId, String envId) {
		//appId = URLEncoder.encode(appId, 'utf-8').replace('+', "%20")
		//envId = URLEncoder.encode(envId, 'utf-8').replace('+', "%20")
		def query = [application: appId, environment: envId]
		def result = xldGenericRestClient.get(
			contentType: ContentType.XML,
			uri: "${xldGenericRestClient.xldUrl}/deployit/deployment/exists",
			query: query
		)
		def val = "${result.text()}"
		
		return val == 'true'

	}
}
