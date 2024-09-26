package com.zions.common.services.slf4j

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
import java.security.KeyStoreException
import org.apache.http.client.ClientProtocolException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import groovyx.net.http.ContentType
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import jakarta.annotation.PostConstruct


@Component
@Slf4j
class LogPubSubGenericRestClient extends AGenericRestClient {
	
	@Value('${pubsub.url:}')
	String pubSubUrl
	
	
	@Value('${webhook.user:}')
	String reUser
	
	@Value('${webhook.password:}')
	String rePassword
	
	@Value('${pubsub.use.proxy:true}')
	Boolean useProxy
	
	
	LogPubSubGenericRestClient() {
		
	}
		
	
	@PostConstruct
	void init() {
		delegate = new ARESTClient(pubSubUrl)
		delegate.ignoreSSLIssues()
		delegate.handler.failure = { resp ->
			if (resp.entity) {
				def outputStream = new ByteArrayOutputStream()
				resp.entity.writeTo(outputStream)
				def errorMsg = outputStream.toString('utf8')
				if (resp.status == 400 || resp.status == 412)
					log.info("SNOW Http ${resp.status} response:  ${errorMsg}")
				else
					log.warn("SNOW Http ${resp.status} error response:  ${errorMsg}")
			}
			return resp
		}
		if (useProxy) {
			setProxy()
		}

		setCredentials(null, null)
	}
	
	void setCredentials(String user, String token) {
		String auth = "$reUser:$rePassword".bytes.encodeBase64()
		delegate.headers['Authorization'] = 'Basic ' + auth
	}
}