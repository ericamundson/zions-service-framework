package com.zions.vsts.services.rmq.mixins

import groovy.json.JsonSlurper
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.amqp.core.MessageListener
import org.springframework.amqp.core.Message
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Value

import com.zions.vsts.services.notification.NotificationService

trait MessageReceiverTrait implements MessageListener {
	private static final String X_RETRIES_HEADER = "x-retries"
	@Autowired(required=false)
	RabbitTemplate rabbitTemplate
	
	@Autowired(required=false)
	NotificationService notificationService
	
	@Value('${queue.name:}')
	String queueName
	
	@Value('${do.retries:true}')
	boolean doRetries

	void onMessage(Message message) {
		try {
			String mStr = new String(message.body)
			def adoData = new JsonSlurper().parseText(mStr)
			processADOData(adoData)
		} catch (e) {
			if (doRetries && !metRetryLimit(message, e)) {
				//throw new AmqpRejectAndDontRequeueException(e.message)
				throw e
			} 
//			else {
//				putIntoDeadQueue(message)
//			}		
		}
		//return 'OK'
	}
	
	private boolean metRetryLimit(Message failedMessage, Throwable exception) {
		Map<String, Object> headers = failedMessage.getMessageProperties().getHeaders();
		Integer retriesHeader = (Integer) headers.get(X_RETRIES_HEADER);
		if (retriesHeader == null) {
			retriesHeader = Integer.valueOf(0);
		}
		if (retriesHeader < 3) {
			headers.put(X_RETRIES_HEADER, retriesHeader + 1);
			headers.put("x-delay", 5000 * retriesHeader);
			this.rabbitTemplate.send('delay-exchange', "${queueName}", failedMessage);
		}
		else {
			headers['x-origin-queue'] = queueName
			headers['x-error'] = "Queue: ${queueName} :: had failed message with error: ${exception.message}"
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			exception.printStackTrace(pw);
			headers['x-trace'] = "${sw}"
			this.rabbitTemplate.send('dead-letter-exchange', 'parked-queue', failedMessage);
			if (notificationService) {
				notificationService.sendMicroServiceIssueNotification(failedMessage)
			}
			return true
		}
		return false
	}
	
	abstract def processADOData(def adoData)
}
