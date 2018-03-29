package com.zions.vsts.services.rest

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient;
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.springframework.beans.factory.annotation.Value

class GenericRestClient {
	private RESTClient delegate;
	
	@Value('${tfs.url}')
	private String tfsUrl;
	
	@Value('${tfs.token')
	private String token

	public GenericRestClient() {
		delegate = new RESTClient(tfsUrl)
		delegate.ignoreSSLIssues()
		delegate.handler.failure = { it }
		setProxy()
		setCredentials(token);

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
	
	void setCredentials(String token) {
		String auth = "$token".bytes.encodeBase64()
		delegate.headers['Authorization'] = 'Basic ' + auth
		
	}
	
	def get(Map input) {
		HttpResponseDecorator resp = delegate.get(input)
		JsonOutput t
		def out = JsonOutput.prettyPrint(JsonOutput.toJson(resp.data))
		println out
		if ("${out}" == 'null') return null
		JsonSlurper sl = new JsonSlurper()
		def oOut = sl.parseText(out)
		return oOut;
	}
	
}
