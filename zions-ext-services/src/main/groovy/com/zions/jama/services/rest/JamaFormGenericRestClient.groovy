package com.zions.jama.services.rest

import com.zions.common.services.rest.AGenericRestClient
import com.zions.common.services.rest.ARESTClient
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient;
import groovy.util.logging.Slf4j;
import java.security.KeyManagementException
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.util.Map
import org.apache.http.Header
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.ClientProtocolException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Handles CLM rest request to reporting host which is base
 * authenticated.
 * 
 * @author z097331
 *
 */
@Component
@Slf4j
class JamaFormGenericRestClient extends AGenericRestClient {
		
	public String jamaUrl;
	
	private String user;
		
	private String password
	
	public String getJamaUrl() {
		return jamaUrl
	}
	
	@Autowired
	public JamaFormGenericRestClient(@Value('${jama.url}') String jamaUrl,
		@Value('${jama.user}') String user,
		@Value('${jama.password}') String password) {
		this.jamaUrl = jamaUrl
		this.password = password;
		this.user = user;
		delegate = new ARESTClient(jamaUrl)
		delegate.ignoreSSLIssues()
		delegate.handler.failure = { it }
		setProxy()
		init()
	}
	
	/* (non-Javadoc)
	 * @see com.zions.vsts.services.tfs.rest.IGenericRestClient#setCredentials(java.lang.String, java.lang.String)
	 */
	@Override
	void setCredentials(String user, String token) {
		// Do nothing.
	}
	def init()
	{
		try {
			
			HttpResponseDecorator resp = this.delegate.get(	
				uri: "${this.jamaUrl}/login.req",
				headers: [Accept: 'text/html',
						 Connection: 'keep-alive',
						 Host: 'zionsbancorp.jamacloud.com',
						 'Upgrade-Insecure-Requests': '1',
						 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:77.0) Gecko/20100101 Firefox/77.0' ]
			);	
			resp = this.delegate.post(
				uri: "${this.jamaUrl}/j_acegi_security_check",
				query: [j_username: this.user, j_password: this.password, j_hash: '/home'],
				requestContentType: 'application/x-www-form-urlencoded',
				headers: [Origin: 'https://zionsbancorp.jamacloud.com',
						 Connection: 'keep-alive',
						 Host: 'zionsbancorp.jamacloud.com',
						 Referer: 'https://zionsbancorp.jamacloud.com/login/req',
						 'Upgrade-Insecure-Requests': '1',
						 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:77.0) Gecko/20100101 Firefox/77.0' ]
			);

			int i = 1
		} catch (NoSuchAlgorithmException e) {
			log.error(e)
		} catch (KeyManagementException e) {
			log.error(e)
		} catch (ClientProtocolException e) {
			log.error(e)
		} catch (IOException e) {
			log.error(e)
		} catch (KeyStoreException e) {
			e.printStackTrace();
			log.error(e)
		}
	}
}
