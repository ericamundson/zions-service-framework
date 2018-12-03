package com.zions.bb.services.rest

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient;

import java.util.Map

import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import com.zions.common.services.rest.AGenericRestClient
import com.zions.common.services.rest.IGenericRestClient

@Component
class BBGenericRestClient extends AGenericRestClient {
	String bbUrl;
	
	private String user;
		
	private String password
	
	public BBGenericRestClient() {}

	@Autowired(required=false)
	public BBGenericRestClient(@Value('${bb.url}') String bbUrl,
		@Value('${bb.user}') String user,
		@Value('${bb.password}') String password) {
		this.bbUrl = bbUrl
		this.password = password;
		this.user = user;
		delegate = new RESTClient(bbUrl)
		delegate.ignoreSSLIssues()
		delegate.handler.failure = { it }
		setProxy()
		setCredentials(user, password);

	}
	void setCredentials(String user, String token) {
		String auth = "$user:$token".bytes.encodeBase64()
		delegate.headers['Authorization'] = 'Basic ' + auth
		
	}
	

}
