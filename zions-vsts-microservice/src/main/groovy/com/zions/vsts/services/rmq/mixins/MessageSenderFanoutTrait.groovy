package com.zions.vsts.services.rmq.mixins
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import groovy.json.JsonBuilder

trait MessageSenderFanoutTrait {
	@Autowired
	RabbitTemplate rabbitTemplate
	
	@Autowired
	RabbitTemplate drRabbitTemplate

	def sendMessage(def adoData, String topic) {
		//String adoMessage = new JsonBuilder(adoData).toString()
		rabbitTemplate.convertAndSend(topic, null, adoData)
	}
	
	def sendDrMessage( def adoData, String topic) {
		drRabbitTemplate.convertAndSend(topic, null, adoData)
		
	}
	
}
