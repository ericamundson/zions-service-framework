package com.zions.rm.services.rest;

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

@Slf4j
@Component
public class RmGenericRestClient implements IGenericRestClient {
	private RESTClient delegate;
	
	String userid;
	
	String password;
	
	public String rmUrl;
	
	public RmGenericRestClient(@Value('${clm.url}') String rmUrl,
		@Value('${clm.user}') String user,
		@Value('${clm.password}') String password) {
		this.userid = user
		this.password = password
		this.rmUrl = rmUrl
		delegate = new RESTClient(rmUrl)
		delegate.ignoreSSLIssues()
		delegate.handler.failure = { it }
		setProxy();
		init();
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
			log.info('stuff')
		}
	}

	def init()
	{
		try {
			
			HttpResponseDecorator resp = this.delegate.get(	
				uri: "${this.rmUrl}/rm/authenticated/identity",
				headers: [Accept: 'text/html']
			);	
			resp = this.delegate.post( 
				uri: "${this.rmUrl}/rm/authenticated/j_security_check",
				query: [j_username: this.userid, j_password: this.password],
				requestContentType: 'application/x-www-form-urlencoded'
			);

		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	def get(Map input) {
		HttpResponseDecorator resp = delegate.get(input)
		
		def out = resp.data;
		//println new groovy.xml.StreamingMarkupBuilder().bindNode(out) as String
		return out;
	}
	def getWResponse(Map input) {
		HttpResponseDecorator resp = delegate.get(input)
		
		return resp;
	}

	def put(Map input) {
		HttpResponseDecorator resp = delegate.put(input)
		
		if (resp.status != 200) {
			return null;
		}
		def out = resp.data;
		return out;
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
		def out = resp.data;
		return out;
	}

	def post(Map input) {
		HttpResponseDecorator resp = delegate.post(input)
		JsonOutput t
		def out = resp.data;
		return out;
	}

	public void setCredentials(String user, String token) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public Object rateLimitPost(Map input) {
		// TODO Auto-generated method stub
		return null;
	}
}

