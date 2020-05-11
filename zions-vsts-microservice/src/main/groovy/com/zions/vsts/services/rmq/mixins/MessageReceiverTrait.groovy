package com.zions.vsts.services.rmq.mixins

import groovy.json.JsonSlurper
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired

trait MessageReceiverTrait {
	
	void receive(def message) {
		try {
			def adoData = new JsonSlurper().parseText(message)
			processADOData(adoData)
		} catch (e) {
			throw e
		}
		//return 'OK'
	}
	abstract def processADOData(def adoData)
}
