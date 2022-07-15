package com.zions.vsts.services.rmq.mixins

import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate

import com.fasterxml.jackson.core.JsonProcessingException

import java.text.SimpleDateFormat

public abstract class MessageConsumer {
	@KafkaListener(topics = ["#{'\${kafka.topic.names}'.split(',')}"], groupId = "\${kafka.topic.group.id}")
	@KafkaListener(id = 'dlq', topics = ["#{'\${kafka.dlq.name}'.split(',')}"], groupId = "\${kafka.topic.group.id}", autoStartup = "\${kafka.dlq.processing}")
	public def receivedMessage(String message) throws JsonProcessingException {
		String mStr = new String(message)
		def adoData = new JsonSlurper().parseText(mStr)
		processADOData(adoData, message)
	}
	
	abstract def processADOData(def adoData, def message)
	
	public def processADOData(def adoData) {
		processADOData(adoData, null)
	}
}
