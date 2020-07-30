package com.zions.vsts.services.rmq.mixins

import groovy.json.JsonSlurper
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.amqp.core.MessageListener
import org.springframework.amqp.core.Message
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Value

import com.zions.vsts.services.notification.NotificationService

trait NotificationReceiverTrait implements MessageListener {
	
	@Autowired(required=false)
	NotificationService notificationService
	

	void onMessage(Message message) {
		try {
			notificationService.sendMicroServiceIssueNotification(message)
		} catch (e) {
//			else {
//				putIntoDeadQueue(message)
//			}		
		}
		//return 'OK'
	}
	
}
