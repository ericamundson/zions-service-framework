package com.zions.xlr.services.rest.client

import com.zions.common.services.rest.AGenericRestClient
import com.zions.common.services.rest.ARESTClient
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient;
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class XlrGenericRestClient extends AGenericRestClient {
	
	String xlrUrl;
	
	
	private String user;
	
	
	private String password
	
	

	@Autowired
	public XlrGenericRestClient(@Value('${xlr.url:}') String url, 
		@Value('${xl.user:}') String user, 
		@Value('${xl.password:}') String password,
		@Value('${xlr.use.proxy:false}') boolean useProxy) {
		this.xlrUrl = url
		this.user = user
		this.password = password
		delegate = new ARESTClient(xlrUrl)
		delegate.ignoreSSLIssues()
		delegate.handler.failure = { it }
		if (useProxy) {
			setProxy()
		}
		setCredentials(user, password);

	}
	
	void setCredentials(String user, String token) {
		String auth = "$user:$token".bytes.encodeBase64()
		delegate.headers['Authorization'] = 'Basic ' + auth
		
	}
	
	
}
