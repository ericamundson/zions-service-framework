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
		@Value('${xlr.user:}') String user, 
		@Value('${xlr.password:}') String password) {
		this.xlrUrl = url
		this.user = user
		this.password = password
		delegate = new ARESTClient(xlrUrl)
		delegate.ignoreSSLIssues()
		delegate.handler.failure = { it }
		setProxy()
		setCredentials(user, password);

	}
	def setProxy() {
		String proxyHost = System.getProperty("proxy.Host")
		if (proxyHost != null) {
			String proxyPort = System.getProperty("proxy.Port")
			String proxyUser = System.getProperty("proxy.User")
			String proxyPassword = System.getProperty("proxy.Password")
			
			delegate.client.getCredentialsProvider().setCredentials(
				new AuthScope(proxyHost, Integer.parseInt(proxyPort)),
				new UsernamePasswordCredentials(proxyUser, proxyPassword)
			)
			delegate.setProxy(proxyHost, Integer.parseInt(proxyPort), 'http')
			
		}
	}
	
	void setCredentials(String user, String token) {
		String auth = "$user:$token".bytes.encodeBase64()
		delegate.headers['Authorization'] = 'Basic ' + auth
		
	}
	
	
}
