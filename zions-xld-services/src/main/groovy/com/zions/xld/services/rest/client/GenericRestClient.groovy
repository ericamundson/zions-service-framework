package com.zions.xld.services.rest.client

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
	
	
	String xldUrl;
	
	private String user;
		
	private String password

	@Autowired
	public GenericRestClient(@Value('${xld.url}') String tfsUrl,
		@Value('${xld.user}') String user,
		@Value('${xld.password}') String password) {
		this.xldUrl = tfsUrl
		this.password = password;
		this.user = user;
		delegate = new RESTClient(tfsUrl)
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
		
		if (resp.status != 200) {
			return null;
		}
		def out = JsonOutput.prettyPrint(JsonOutput.toJson(resp.data))
		if ("${out}" == 'null') return null
		JsonSlurper sl = new JsonSlurper()
		def oOut = sl.parseText(out)
		return oOut;
	}
	
	def delete(Map input) {
		HttpResponseDecorator resp = delegate.delete(input)
		if (resp.status != 204) {
			return null;
		}
	}
	
	def patch(Map input) {
		HttpResponseDecorator resp = delegate.patch(input)
		
		if (resp.status != 200) {
			return null;
		}
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
