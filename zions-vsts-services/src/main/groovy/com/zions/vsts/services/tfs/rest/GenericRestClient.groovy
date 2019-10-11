package com.zions.vsts.services.tfs.rest

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
import org.apache.http.client.HttpClient
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
class GenericRestClient extends AGenericRestClient {
		
	String tfsUrl;
	
	private String user;
		
	private String token
	
	public String getTfsUrl() {
		return tfsUrl
	}
	
	/**
	 * For unit testing
	 */
	public GenericRestClient(RESTClient client) {
		delegate = client
		checked = true
	}

	@Autowired
	public GenericRestClient(@Value('${tfs.url}') String tfsUrl,
		@Value('${tfs.user}') String user,
		@Value('${tfs.token}') String token) {
		this.tfsUrl = tfsUrl
		this.token = token;
		this.user = user;
		delegate = new RESTClient(tfsUrl)
		delegate.ignoreSSLIssues()
		delegate.handler.failure = { it }
		setProxy()
		setCredentials(user, token);
		checked = true;
		//retryConnect()
	}
	
	private retryConnect() {
		def result = null
		for (int i = 0; i < 5; i++) {
			try {
				result = delegate.get(
					contentType: 'application/json',
					uri: "${tfsUrl}/_apis/projects",
					headers: [Accept: 'application/json'],
					query: ['api-version': '5.0']
					)
				
				break;
			} catch (javax.net.ssl.SSLHandshakeException e) {
				log.error('SSL handshake failed!')
				System.sleep((i+1)*2000)
			}
		}
		if (!result) throw new Exception("Failed to connect to ADO!")
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
