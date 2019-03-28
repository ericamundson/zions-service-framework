package com.zions.vsts.ws;

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import javax.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.client.RestTemplate
import groovy.util.logging.Slf4j
import groovy.json.JsonSlurper
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.codec.binary.Base64;

/**
 * ReST Controller for forwarding Azure DevOps through subscriptions via websockets.
 * 
 * @author Eric Amundson
 * 
 */
@RestController
@Slf4j
public class EventController {
	
	@Autowired
	private SimpMessageSendingOperations messagingTemplate;


    /**
     * Parse event type from request body and publish to subscribed clients. 
     *  
     * @return
     */
    @RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity forwardADOEvent(@RequestBody String body, HttpMethod method, HttpServletRequest request) throws URISyntaxException {
		log.debug("EventController::forwardADOEvent - Request body:\n"+body)
		def eventData = new JsonSlurper().parseText(body)
		// handle multiple service mappings for a single event type
		//def serviceDetails = getServiceDetails("${eventData.eventType}")
		String eventType = "${eventData.eventType}"
		String topic = "/topic/${eventType}"
		messagingTemplate.convertAndSend(topic, body)
		String result = "Published ${eventType}"
		return new ResponseEntity<String>(result, HttpStatus.OK)
		//return ResponseEntity.ok(HttpStatus.OK)
	}


 }
