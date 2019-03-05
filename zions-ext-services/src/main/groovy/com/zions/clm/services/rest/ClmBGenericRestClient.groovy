package com.zions.clm.services.rest

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
 * Provides behavior to interact with TFS/VSTS rest api.  It utilizes groovy RESTClient
 * that provides a bunch of automation to take and output an object structure that simplifies or 
 * reduces need to build a BOM to represent data transfer object layer.
 * 
 * @author z091182
 *
 */
@Component
@Slf4j
class ClmBGenericRestClient extends AGenericRestClient {
		
	public String tfsUrl;
	
	private String user;
		
	private String password
	
	public String getClmUrl() {
		return tfsUrl
	}
	
	/**
	 * For unit testing
	 */
	public ClmBGenericRestClient(RESTClient client) {
		delegate = client
		checked = true
	}

	@Autowired
	public ClmBGenericRestClient(@Value('${clm.url}') String clmUrl,
		@Value('${clm.user}') String user,
		@Value('${clm.password}') String password) {
		this.tfsUrl = clmUrl
		this.password = password;
		this.user = user;
		delegate = new RESTClient(clmUrl)
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
