package com.zions.vsts.pubsub;

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
import com.zions.vsts.services.rmq.mixins.MessageSenderFanoutTrait

/**
 * ReST Controller for forwarding Azure DevOps through subscriptions via RabbitMQ.
 * 
 * @author Eric Amundson
 * 
 */
@RestController
@Slf4j
public class EventController implements MessageSenderFanoutTrait {
	
	ResponseEntity<String> okresult = new ResponseEntity<String>('All OK', HttpStatus.OK)

    /**
     * Parse event type from request body and publish to subscribed clients. 
     *  
     * @return
     */
    @RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity forwardADOEvent(@RequestBody String body, HttpMethod method, HttpServletRequest request) throws URISyntaxException {
		//log.debug("EventController::forwardADOEvent - Request body:\n"+body)
		def eventData = new JsonSlurper().parseText(body)
		// handle multiple service mappings for a single event type
		//def serviceDetails = getServiceDetails("${eventData.eventType}")
		String eventType = "${eventData.eventType}"
		//String topic = "/topic/${eventType}"
		try {
			sendMessage(body, eventType) 
		} catch (e) {
			log.error("Failed with event type: ${eventType}.  Probably no exchange setup yet.  Error:  ${e.message}")
			String result = "Failed with event type: ${eventType}. Error:  ${e.message}"
			return new ResponseEntity<String>(result, HttpStatus.EXPECTATION_FAILED)
		}
		// Removed 'new ResponseEntity<String>(result, HttpStatus.OK)' call everytime to reduce heap issue potential.
		return okresult
		//return ResponseEntity.ok(HttpStatus.OK)
	}


 }
