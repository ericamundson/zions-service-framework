package com.zions.vsts.services.rmq.mixins

import java.util.HashMap
import java.util.Map

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.clients.CommonClientConfigs

import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.LoggingProducerListener
import org.springframework.kafka.support.ProducerListener
import org.springframework.kafka.support.serializer.JsonDeserializer

trait MessageProducerConfigTrait {

	@Value('${kafka.servers:}')
	String kafkaServers
	
	@Value('${kafka.clientid:}')
	String kafkaClientId
	
	@Value('${kafka.max-block-ms:}')
	Long kafkaMaxBlockMs
	
	@Value('${kafka.keytab:}')
	private String keytab;
	
	@Value('${kafka.krb5.conf:}')
	private String krb5conf;

	/**
	 * Create a {@link KafkaTemplate} to be made available in the Spring Application Context
	 * @return a configured template instance
	 */
	@Bean
	public KafkaTemplate<String, String> kafkaTemplate() {
		
		// Create the template
		KafkaTemplate<String, String> template = new KafkaTemplate<String, String>(producerFactory())
		
		return template
	}

	/**
	 * Create a default Kafka ProducerFactory
	 * @return
	 */
	@Bean
	public ProducerFactory<String, String> producerFactory() {
		// Create property map
		Map<String, Object> props = new HashMap<>()
		
		System.setProperty("java.security.auth.login.config", keytab)
		System.setProperty("sun.security.krb5.debug","false")
		System.setProperty("java.security.krb5.conf", krb5conf)
		
		// List of host:port pairs used for establishing the initial connections to the Kafka cluster
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers)
		
		// Set client id, meta data for kafka logging
		props.put(ProducerConfig.CLIENT_ID_CONFIG, kafkaClientId)
		
		// Serializer classes for key and value
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
		props.put(JsonDeserializer.TRUSTED_PACKAGES, "*")
		
		// Value, in milliseconds, to block, after which it will throw a TimeoutException
		props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, kafkaMaxBlockMs)
		
		// Add SSL Protocol
		props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
		props.put("sasl.mechanism", "GSSAPI")
		
		return new DefaultKafkaProducerFactory<String, String>(props)
	}
}