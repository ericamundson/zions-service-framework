package com.zions.jama.services.rest

import com.zions.common.services.rest.AGenericRestClient
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient;
import groovy.util.logging.Slf4j;

import java.util.Map
import org.apache.http.Header
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Handles Jama rest request to reporting host which is base
 * authenticated.
 * 
 * @author z097331
 *
 */
@Component
@Slf4j
class JamaGenericRestClient extends AGenericRestClient {
		
	public String jamaUrl;
	
	private String user;
		
	private String password
	
	public String getJamaUrl() {
		return jamaUrl
	}
	
	/**
	 * For unit testing
	 */
	public JamaGenericRestClient(RESTClient client) {
		delegate = client
		checked = true
	}

	@Autowired
	public JamaGenericRestClient(@Value('${jama.url}') String jamaUrl,
		@Value('${jama.user}') String user,
		@Value('${jama.password}') String password) {
		this.jamaUrl = jamaUrl
		this.password = password;
		this.user = user;
		delegate = new RESTClient(jamaUrl)
		delegate.ignoreSSLIssues()
		delegate.handler.failure = { it }
		//setProxy()
		setCredentials(user, password);
		checked = true;
	}
	
	/* (non-Javadoc)
	 * @see com.zions.vsts.services.tfs.rest.IGenericRestClient#setCredentials(java.lang.String, java.lang.String)
	 */
	@Override
	void setCredentials(String user, String token) {
		String auth = "$user:$token".bytes.encodeBase64()
		delegate.headers['Authorization'] = 'Basic ' + auth
		
	}
	
}
