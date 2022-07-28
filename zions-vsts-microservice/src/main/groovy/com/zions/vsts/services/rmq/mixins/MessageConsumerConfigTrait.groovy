package com.zions.vsts.services.rmq.mixins

import java.util.HashMap
import java.util.Map
import java.time.Duration
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.clients.CommonClientConfigs
import org.springframework.kafka.listener.ContainerProperties.AckMode
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.DependsOn
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.SeekToCurrentErrorHandler
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.util.backoff.FixedBackOff


trait MessageConsumerConfigTrait {
	@Value('${kafka.servers:}')
	String servers
	@Value('${kafka.groupid:}')
	String group
	@Value('${kafka.dlq.name}')
	String dlqTopic
	@Value('${kafka.dlq.processing}')
	Boolean dlqProcessing
	/**
	 * Create a default Kafka ConsumerFactory
	 * @return
	 */
	@Bean
	@DependsOn("kafkaInitializer")
	public ConsumerFactory<String, String> consumerFactory() {
		Map<String, Object> props = new HashMap<>();
				
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers)
		props.put(ConsumerConfig.GROUP_ID_CONFIG, group)
		props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, 'SASL_SSL')
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
		props.put("group.id", "group")
		props.put("sasl.mechanism", 'GSSAPI')
		
		// Deserializer classes for key and value
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class)
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class)
		props.put(JsonDeserializer.TRUSTED_PACKAGES, "*")

		return new DefaultKafkaConsumerFactory<>(props)
	}
	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
			ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
			ConsumerFactory<String, String> consumerFactory,
			KafkaTemplate<String, String> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>()
        configurer.configure(factory, consumerFactory)
        factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE)
		/*
       	factory.setConsumerFactory(consumerFactory())
        factory.getContainerProperties().setSyncCommits(true)
        factory.setAckDiscarded(true)
        factory.getContainerProperties().setAuthorizationExceptionRetryInterval(Duration.ofMillis(20000))
        factory.setRecordFilterStrategy(recordFilterStrategy)
		*/
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                {r, e -> new TopicPartition("$dlqTopic", -1)}
        )
		if (dlqProcessing) {
			println("=================================================================")
			println("= Warning:  DLQ Processing is turned on!!!")
			println("=           Remember to turn off and restart service when done.")
			println("=           The setting can be found in ConfigMap: dlq-config.")
			println("=================================================================")
		}
        SeekToCurrentErrorHandler seekToCurrentErrorHandler = new SeekToCurrentErrorHandler(recoverer,
                new FixedBackOff(0L, 3L))
        seekToCurrentErrorHandler.setCommitRecovered(true)
        factory.setErrorHandler(seekToCurrentErrorHandler)

        return factory;
    }	
}