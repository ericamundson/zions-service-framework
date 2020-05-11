package com.zions.vsts.services.rmq.mixins
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import groovy.json.JsonBuilder

trait MessageSenderTrait {
	@Autowired
	RabbitTemplate rabbitTemplate
	
	@Value('${routing.exchange.name:}')
	String routingExchangeName
	
	@Value('${routing.direct.key:}')
	String routingDirectKey

	def sendMessage(def adoData) {
		//String adoMessage = new JsonBuilder(adoData).toString()
		rabbitTemplate.convertAndSend(routingExchangeName, routingDirectKey, adoData)
	}
	
	
}
