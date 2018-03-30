package com.zions.vsts.services.tfs.rest

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
class GenericRestClient {
	private RESTClient delegate;
	
	
	String tfsUrl;
	
	private String user;
		
	private String token

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
	
	def get(Map input) {
		HttpResponseDecorator resp = delegate.get(input)
		JsonOutput t
		def out = JsonOutput.prettyPrint(JsonOutput.toJson(resp.data))
		if ("${out}" == 'null') return null
		JsonSlurper sl = new JsonSlurper()
		def oOut = sl.parseText(out)
		return oOut;
	}
	
	def put(Map input) {
		HttpResponseDecorator resp = delegate.put(input)
		JsonOutput t
		def out = JsonOutput.prettyPrint(JsonOutput.toJson(resp.data))
		if ("${out}" == 'null') return null
		JsonSlurper sl = new JsonSlurper()
		def oOut = sl.parseText(out)
		return oOut;
	}
	
	def post(Map input) {
		HttpResponseDecorator resp = delegate.post(input)
		JsonOutput t
		def out = JsonOutput.prettyPrint(JsonOutput.toJson(resp.data))
		if ("${out}" == 'null') return null
		JsonSlurper sl = new JsonSlurper()
		def oOut = sl.parseText(out)
		return oOut;
	}
}
