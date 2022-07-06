package com.zions.vsts.services.rmq.mixins

import org.springframework.context.annotation.Bean

trait KafkaInitializerConfigTrait {
	@Bean
	public KafkaInitializer kafkaInitializer() {
		return new KafkaInitializer()
	}
}