package com.zions.xld.services.rest.client

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
class XldGenericRestClient extends AGenericRestClient {
	
	String xldUrl;
	
	
	private String user;
	
	
	private String password

	@Autowired
	public XldGenericRestClient(
		@Value('${xld.url:}') String url, 
		@Value('${xl.user:}') String user, 
		@Value('${xl.password:}') String password,
		@Value('${xld.use.proxy:false}') boolean useProxy) {
		this.xldUrl = url
		this.user = user
		this.password = password
		delegate = new ARESTClient(xldUrl)
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
