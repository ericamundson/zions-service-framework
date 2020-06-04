package com.zions.vsts.services.ws.client

import com.zions.common.services.rest.AGenericRestClient
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import groovyx.net.http.RESTClient
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.NTCredentials
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.HttpClient
import org.apache.http.conn.params.ConnRoutePNames
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.conn.ssl.TrustStrategy
import org.apache.http.conn.ssl.X509HostnameVerifier
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.ProxyAuthenticationStrategy
import org.apache.http.ssl.SSLContextBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.lang.Nullable
import org.springframework.messaging.converter.StringMessageConverter
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.client.WebSocketClient
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.Transport
import org.springframework.web.socket.sockjs.client.WebSocketTransport
import java.lang.reflect.Type;
import java.security.KeyManagementException
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.UnrecoverableKeyException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLException
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Abstract definition to setup a VSTS websocket client micro-service to handle ADO web hook events.
 * 
 * @author z091182
 *
 */
@Slf4j
abstract class AbstractWebSocketMicroService extends StompSessionHandlerAdapter {
    public static final String SSL_CONTEXT_PROPERTY =
            "org.apache.tomcat.websocket.SSL_CONTEXT";
	TaskScheduler heartBeatScheduler
	
	StompSession session
	String websocketUrl
	String websocketUser
	String websocketPassword
	WebSocketStompClient stompClient

	public AbstractWebSocketMicroService(websocketUrl, 
		websocketUser = null, 
		websocketPassword = null ) {
		this.websocketUrl = websocketUrl
		this.websocketUser = websocketUser
		this.websocketPassword = websocketPassword
		this.heartBeatScheduler = new ThreadPoolTaskScheduler();
		connect()
		startCheck();
	}
	public AbstractWebSocketMicroService() {
		// Constructor for unit testing of microservices
	}
		
	private RestTemplate getRestTemplate() {
		HttpClientBuilder clientBuilder = HttpClientBuilder.create();
		String proxyHost = System.getProperty("wsproxy.Host")
		CloseableHttpClient client = null;
		if (proxyHost != null) {
			String proxyUser = System.getProperty("wsproxy.User");
			String proxyPassword = System.getProperty("wsproxy.Password");
			int proxyPort = Integer.parseInt(System.getProperty("wsproxy.Port"));
			CredentialsProvider credentialsProvider = null
			if (proxyUser != null) {
				credentialsProvider = new BasicCredentialsProvider();
				credentialsProvider.setCredentials(
						new AuthScope(proxyHost, proxyPort),
						new NTCredentials("${proxyUser}:${proxyPassword}"));
			}
	
			HttpHost myProxy = new HttpHost(proxyHost, proxyPort, 'http');
			clientBuilder.setProxy(myProxy)
			if (credentialsProvider != null) {
					clientBuilder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy())
					clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
			}
			clientBuilder.disableCookieManagement()
			this.setupSSL(clientBuilder)
			client = clientBuilder.build()
//			WebGenericRestClient c = new WebGenericRestClient(websocketUrl)
//			client = c.delegate.client
		} else {
			this.setupSSL(clientBuilder)
			client = clientBuilder.build();
		}
		
		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
		factory.setHttpClient(client);
		return new RestTemplate(factory);
	}
	
	private def startCheck() {
		Runnable check = new Runnable() {
			public void run() {
				while (true) {
					try {
						System.sleep(10000)
						if (session == null || !session.isConnected()) {
							AbstractWebSocketMicroService.this.connect();
						}
					} catch (Exception e) {
						log.error("Error while re-connecting websocket session: {}", e);
					}
				}
			}
		}
		Thread checkThread = new Thread(check);
		checkThread.start()

	}
	
	public def connect() {
		StandardWebSocketClient client = new StandardWebSocketClient();
		setupSSL(client);
//		List<Transport> webSocketTransports = Arrays.asList(new WebSocketTransport(client),  new RestTemplateXhrTransport(getRestTemplate()));
		List<Transport> webSocketTransports = Arrays.asList(new RestTemplateXhrTransport(getRestTemplate()));
		SockJsClient sockJsClient = new SockJsClient(webSocketTransports);
		stompClient = new WebSocketStompClient(sockJsClient);
		stompClient.setMessageConverter(new StringMessageConverter());
		stompClient.setAutoStartup(true);
		if (this.websocketUser && this.websocketPassword) {
			String plainCredentials="${websocketUser}:${websocketPassword}";
			String base64Credentials = Base64.getEncoder().encodeToString(plainCredentials.getBytes());
			
			final WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
			headers.add("Authorization", "Basic " + base64Credentials);
			stompClient.connect(websocketUrl, headers, this);
		} else {
			stompClient.connect(websocketUrl, this);
			
		}
	}
	
	@Override
	public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
		log.info("New session established : " + session.getSessionId());
		if (topics()) {
			for (String aTopic in topics()) {
				session.subscribe("/topic/${aTopic}", this);
			}
		} else {
			String aTopic = topic()
			session.subscribe("/topic/${aTopic}", this);
		}
		this.session = session;
	}
	
	@Override
	public Type getPayloadType(StompHeaders headers) {
		return String.class;
	}

	@Override
	public void handleFrame(StompHeaders headers, Object payload) {
		String adoDataString = (String) payload;
		def adoData = new JsonSlurper().parseText(adoDataString)
		processADOData(adoData)
	}
	
	/**
	 * Implemented by ADO micro-service clients to handle ADO web hook data.
	 * @param adoData
	 * @return
	 */
	abstract def processADOData(def adoData);
	
	/**
	 * The ADO web hook event type to subscribe.
	 * E.G.  'workitem.updated'
	 * @return
	 */
	abstract String topic();
	
	String[] topics() {
		return null
	}
	
	@Override
	public void handleException(StompSession session, @Nullable StompCommand command,
			StompHeaders headers, byte[] payload, Throwable exception) {
			log.error("WS Exception", exception)
	}

	/**
	 * This implementation is empty.
	 */
	@Override
	public void handleTransportError(StompSession session, Throwable exception) {
			log.error("Transport fail", exception.message)
	}
		
	void setupSSL(HttpClientBuilder clientBuilder)
			throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException{
       TrustStrategy trustStrat = new TrustStrategy(){
            public boolean isTrusted(X509Certificate[] chain, String authtype)
                  throws CertificateException {
                         return true;
                  }
        };
	    SSLContext sslContext = new SSLContextBuilder()
	      .loadTrustMaterial(trustStrat)
	      .build();
		  //NoopHostnameVerifier v
	    SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
		clientBuilder.setSSLSocketFactory(socketFactory)
	
	}
	
    void setupSSL(StandardWebSocketClient wsClient)
	throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException{
      TrustStrategy trustStrat = new TrustStrategy(){
            public boolean isTrusted(X509Certificate[] chain, String authtype)
                  throws CertificateException {
                         return true;
                  }
        };
		SSLContext sslContext = new SSLContextBuilder()
		  .loadTrustMaterial(trustStrat)
		  .build();
		  //NoopHostnameVerifier v
		wsClient.userProperties.put(SSL_CONTEXT_PROPERTY, sslContext);
	}




}
