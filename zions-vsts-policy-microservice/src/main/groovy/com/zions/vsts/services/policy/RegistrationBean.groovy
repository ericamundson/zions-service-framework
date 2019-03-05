package com.zions.vsts.services.policy

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component;
import groovy.json.JsonBuilder
import static groovyx.net.http.ContentType.JSON
import groovyx.net.http.RESTClient;
import groovy.util.logging.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.ContextStoppedEvent
import org.springframework.context.event.EventListener

/**
 * @author z091556
 * 
 */
@Component
@Slf4j
public class RegistrationBean {

	private String regUrl

	private RESTClient rc

	private svcId = ""
	
	@Autowired
	public RegistrationBean(@Value('${reg.url}') String regUrl,
			@Value('${reg.user}') String regUsername, 
			@Value('${reg.password}') String regPassword) {
		this.regUrl = regUrl
		rc = new RESTClient(regUrl)
		String auth = "$regUsername:$regPassword".bytes.encodeBase64()
		rc.headers['Authorization'] = 'Basic ' + auth
	}

	@EventListener(ApplicationReadyEvent.class)
	public void registerAppAsService() {
	    log.debug("In registerAppAsService after Policy service startup")
		def regObj = [eventType: "git.push", serviceName: "policy", protocol: "http", serverUrl: "utmvti0192", port: 9091]
		def body = new JsonBuilder(regObj).toPrettyString()
		log.debug("Registration URL is: ${regUrl}/register")
		def resp = rc.post(
			contentType: JSON,
			path: "/register",
			body: body,
			headers: [Accept: 'application/json'],
		)
		if (resp.status != 200) {
			log.debug("registerAppAsService -- Service Registration Failed. Status: "+resp.getStatusLine());
		}
		def result = resp.data
		log.debug("Policy service successfully registered.  Service UUID: ${result.svcUUID}")
		this.svcId = result.svcUUID
	}

	@EventListener(ContextClosedEvent.class )
	public void unregisterOnClose() {
	    log.debug("In unregisterAppAsService after Policy service context is stopped or closed")
		doUnregister()
	}

	@EventListener(ContextStoppedEvent.class)
	public void unregisterOnStop() {
	    log.debug("In unregisterAppAsService after Policy service context is stopped or closed")
		doUnregister()
	}

	private void doUnregister() {
	    log.debug("In unregisterAppAsService after Policy service context is stopped or closed")
		def unregObj = [eventType: "git.push", svcUUID: this.svcId]
		def body = new JsonBuilder(unregObj).toPrettyString()
		//log.debug("Registration URL is: ${regUrl}/register")
		def resp = rc.post(
			contentType: JSON,
			path: "/unregister",
			body: body,
			headers: [Accept: 'application/json'],
		)
		if (resp.status != 200) {
			log.debug("unregisterAppAsService -- Service De-registration Failed. Status: "+resp.getStatusLine());
		}
		log.debug("Policy service successfully unregistered.")
	}

}
