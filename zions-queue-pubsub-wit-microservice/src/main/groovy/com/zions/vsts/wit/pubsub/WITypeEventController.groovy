package com.zions.vsts.wit.pubsub;

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
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.codec.binary.Base64;
import com.zions.vsts.services.rmq.mixins.MessageSenderFanoutTrait
import com.zions.vsts.services.rmq.mixins.MessageReceiverTrait
import org.springframework.stereotype.Component

/**
 * ReST Controller for forwarding Azure DevOps through subscriptions via RabbitMQ.
 * 
 * @author Eric Amundson
 * 
 */

@Component
@Slf4j
public class WITypeEventController implements MessageSenderFanoutTrait,MessageReceiverTrait {


     public Object processADOData(Object eventData) {

            //detect the work item type involved in the edit
		 	if (!eventData.resource.revision.fields) return
            String wiType = "${eventData.resource.revision.fields.'System.WorkItemType'}"
			wiType = wiType.replace(' ','_')
			boolean isStateChange = (	eventData.resource.fields && 
										eventData.resource.fields.'System.State' && 
										eventData.resource.fields.'System.State'.oldValue)

            //String topic = "/topic/${eventType}"

            String body = new JsonBuilder(eventData).toString()

            try {

                  sendMessage(body, wiType)
				  if (isStateChange)
					  sendMessage(body, wiType + '-statechange')

            } catch (e) {

            }

            return 'message sent'

            //return ResponseEntity.ok(HttpStatus.OK)

      }

 

 

}