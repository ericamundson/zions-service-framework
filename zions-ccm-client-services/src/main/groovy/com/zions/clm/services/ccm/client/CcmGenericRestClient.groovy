package com.zions.clm.services.ccm.client;

import com.zions.common.services.rest.AGenericRestClient
import com.zions.common.services.rest.IGenericRestClient
import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest
import org.apache.http.HttpRequestInterceptor
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URIBuilder
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams
import org.apache.http.params.HttpParams
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Rest client for clm requests.
 * 
 * @author z091182
 *
 */
@SuppressWarnings("deprecation")
@Slf4j
@Component
public class CcmGenericRestClient extends AGenericRestClient {
	
	public String userid = "";
	String password = "";
	public String clmUrl = "";
	
	@Autowired
	public CcmGenericRestClient(@Value('${clm.url}') String clmUrl, @Value('${clm.user}') String userid, @Value('${clm.password}') String password) {
		this.clmUrl = clmUrl;
		this.userid = userid;
		this.password = password;
		delegate = new RESTClient(clmUrl)
		delegate.ignoreSSLIssues()
		delegate.handler.failure = { it }
		//setProxy();
		init();
	}

	def init()
	{
		try {
			
			HttpResponseDecorator resp = this.delegate.get(	
				uri: "${this.clmUrl}/ccm/authenticated/identity",
				headers: [Accept: 'text/html']
			);	
			resp = this.delegate.post( 
				uri: "${this.clmUrl}/ccm/authenticated/j_security_check",
				query: [j_username: this.userid, j_password: this.password],
				requestContentType: 'application/x-www-form-urlencoded'
			);

		} catch (NoSuchAlgorithmException e) {
			log.error(e.message)
		} catch (KeyManagementException e) {
			log.error(e.message)
		} catch (ClientProtocolException e) {
			log.error(e.message)
		} catch (IOException e) {
			log.error(e.message)
		} catch (KeyStoreException e) {
			log.error(e.message)
		}
	}
	

	public void setCredentials(String user, String token) {
		// Do nothing.
		
	}
}



