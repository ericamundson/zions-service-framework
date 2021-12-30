package com.zions.vsts.services.rmq.mixins
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import groovy.json.JsonBuilder

trait MessageSenderREFanoutTrait {
	
	@Autowired
	@Qualifier('k8sRabbitTemplate')
	RabbitTemplate k8sRabbitTemplate

	
	def sendREMessage( def adoData, String topic) {
		k8sRabbitTemplate.convertAndSend(topic, null, adoData)
		
	}
	
}
