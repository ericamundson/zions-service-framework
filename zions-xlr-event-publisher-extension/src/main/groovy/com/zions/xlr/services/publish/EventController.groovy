package com.zions.xlr.services.publish;

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component;
import groovy.util.logging.Slf4j
import groovy.json.JsonBuilder
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.codec.binary.Base64;

import com.xebialabs.xlrelease.domain.events.ActivityLogEvent
import com.zions.vsts.services.rmq.mixins.MessageSenderFanoutTrait
//import com.zions.xlr.services.query.ReleaseQueryService

/**
 * ReST Controller for forwarding Azure DevOps through subscriptions via RabbitMQ.
 * 
 * @author Eric Amundson
 * 
 */
@Component
@Slf4j
public class EventController implements MessageSenderFanoutTrait {
	

    /**
     * Parse event type from request body and publish to subscribed clients. 
     *  
     * @return
     */
	public void forwardXLREvent(ActivityLogEvent eventData)  {
		String activityType = "${eventData.activityType}"
		//String topic = "/topic/${eventType}"
		def ed = [ activityType: eventData.activityType, eventTime: eventData.eventTime, id: eventData.id, message: eventData.message, releaseId: eventData.releaseId, username: eventData.username ]
		String body = new JsonBuilder(ed).toPrettyString()
		//def release = releaseQueryService.getRelease(ed.releaseId)
		
		try {
			sendMessage(body, activityType) 
		} catch (e) {
			//log.error("Failed with event type: ${activityType}.  Probably no exchange setup yet.  Error:  ${e.message}")
			String result = "Failed with event type: ${activityType}. Error:  ${e.message}"
		}
		String result = "Published ${eventData.activityType}"
	}


 }
