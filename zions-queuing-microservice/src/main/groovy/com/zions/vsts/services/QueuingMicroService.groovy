package com.zions.vsts.services

import com.zions.vsts.services.rmq.mixins.MessageReceiverTrait
import com.zions.vsts.services.work.calculations.RollupManagementService
import com.zions.vsts.services.ws.client.WebSocketMicroServiceTrait
import com.zions.vsts.services.rmq.mixins.MessageSenderTrait

import groovy.util.logging.Slf4j
import groovy.json.JsonBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Handles enqueuing ado messages to select message queues for use by specific
 * micro-services to perform actions on those messages.
 * 
 * @author z091182
 *
 */
@Component
@Slf4j
class QueuingMicroService implements WebSocketMicroServiceTrait, MessageSenderTrait {

	
	@Value('${ado.topics:}')
	String[] adoTopics
	
	@Autowired
	public QueuingMicroService(@Value('${websocket.url:}') websocketUrl, 
		@Value('${websocket.user:#{null}}') websocketUser,
		@Value('${websocket.password:#{null}}') websocketPassword) {
		init(websocketUrl, websocketUser, websocketPassword)
		
	}
	
	public def processADOData(def adoData) {
		String adoJson = new JsonBuilder(adoData).toPrettyString()
		sendMessage(adoJson)
	}

	public String topic() {
		return null;
	}
	
    public String[] topics() {
		return adoTopics
	}
}

