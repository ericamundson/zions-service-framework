package com.zions.vsts.services.rmq.mixins
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import groovy.json.JsonBuilder

trait MessageSenderDirectTrait {
	@Autowired
	RabbitTemplate rabbitTemplate
	

	def sendMessage(def adoData, String routingExchangeName, String routingKey) {
		//String adoMessage = new JsonBuilder(adoData).toString()
		rabbitTemplate.convertAndSend(routingExchangeName, routingKey, adoData)
	}
	
	
}
