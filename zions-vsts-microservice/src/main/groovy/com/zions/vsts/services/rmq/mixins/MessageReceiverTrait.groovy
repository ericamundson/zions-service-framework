package com.zions.vsts.services.rmq.mixins

import groovy.json.JsonSlurper
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.amqp.core.MessageListener
import org.springframework.amqp.core.Message
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Value

trait MessageReceiverTrait implements MessageListener {
	@Autowired
	RabbitTemplate rabbitTemplate
	
	@Value('${queue.name:}')
	String queueName

	void onMessage(Message message) {
		try {
			String mStr = new String(message.body)
			def adoData = new JsonSlurper().parseText(mStr)
			processADOData(adoData)
		} catch (e) {
			if (!message.getMessageProperties().redelivered) {
				//throw new AmqpRejectAndDontRequeueException(e.message)
				throw e
			} else {
				putIntoDeadQueue(message)
			}
		}
		//return 'OK'
	}
	
	private void putIntoDeadQueue(Message failedMessage) {
		this.rabbitTemplate.send('dead-letter-exchange', "${queueName}-dead", failedMessage);
	}
	
	abstract def processADOData(def adoData)
}
