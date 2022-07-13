package com.zions.vsts.services.rmq.mixins

import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate

import com.fasterxml.jackson.core.JsonProcessingException

import java.text.SimpleDateFormat

trait MessageConsumerSpecialTrait {
	@KafkaListener(topics = ["#{'\${kafka.topic.names}'.split(',')}"], groupId = "\${kafka.topic.group.id}")
	public def receivedMessage(String message) throws JsonProcessingException {
		String mStr = new String(message)
		def adoData = new JsonSlurper().parseText(mStr)
		processADOData(message, adoData)
	}
	
	abstract def processADOData(def message, def adoData)
}
