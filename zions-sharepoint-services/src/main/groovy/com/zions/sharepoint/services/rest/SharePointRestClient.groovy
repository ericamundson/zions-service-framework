package com.zions.sharepoint.services.rest

import com.zions.common.services.rest.AGenericRestClient
import com.zions.common.services.rest.ARESTClient
import com.zions.common.services.rest.IGenericRestClient
import com.zions.sharepoint.services.rest.IFailureHandler

import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType
import groovyx.net.http.RESTClient

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
@Slf4j
class SharePointRestClient extends AGenericRestClient {
	String graphHost
	String zionsHost
	String user;
	String token
	
	@Autowired
	public SharePointRestClient(String graphHost,
								String zionsHost,
								String user,
								String token) {

		this.graphHost = graphHost
		this.zionsHost = zionsHost
		this.user = user
		delegate = new ARESTClient(url)
		delegate.ignoreSSLIssues()
		delegate.handler.failure = { resp ->
			if (resp.entity) {
				def outputStream = new ByteArrayOutputStream()
				resp.entity.writeTo(outputStream)
				def errorMsg = outputStream.toString('utf8')
				if (resp.status == 400 || resp.status == 412)
					log.info("ADO Http ${resp.status} response:  ${errorMsg}")
				else
					log.error("ADO Http ${resp.status} error response:  ${errorMsg}")
			}
			return resp
		}
		setProxy();
		setCredentials(user, token)
	}
	
	public String getGraphHost() {
		return graphHost
	}
	public String getZionsHost() {
		return zionsHost
	}
	@Override
	void setCredentials(String user, String token) {
		String auth = "$user:$token".bytes.encodeBase64()
		delegate.headers['Authorization'] = 'Basic ' + auth
		
	}
}
