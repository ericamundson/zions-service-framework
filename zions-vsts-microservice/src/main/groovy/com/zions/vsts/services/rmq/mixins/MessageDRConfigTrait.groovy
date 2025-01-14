package com.zions.vsts.services.rmq.mixins

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.AmqpAdmin
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.retry.interceptor.RetryOperationsInterceptor
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.AcknowledgeMode

trait MessageDRConfigTrait {

	@Value('${spring.drrabbitmq.host:localhost}')
	String drhost;
	@Value('${spring.rabbitmq.username:}')
	String username;
	@Value('${spring.rabbitmq.password:}')
	String password;
	
	@Value('${spring.rabbitmq.host:localhost}')
	String host;

	CachingConnectionFactory drConnectionFactory() {
		CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(drhost);
		cachingConnectionFactory.setUsername(username);
		cachingConnectionFactory.setPassword(password);
		return cachingConnectionFactory;
	}
	
	CachingConnectionFactory connectionFactory() {
		CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(host);
		cachingConnectionFactory.setUsername(username);
		cachingConnectionFactory.setPassword(password);
		return cachingConnectionFactory;
	}

	@Bean
	@Qualifier('drRabbitTemplate')
	public RabbitTemplate drRabbitTemplate() {
		final RabbitTemplate rabbitTemplate = new RabbitTemplate(drConnectionFactory());
		//rabbitTemplate.setMessageConverter(jsonMessageConverter());
		return rabbitTemplate;
	}
	
	//@Primary
	@Bean
	@Qualifier('rabbitTemplate')
	public RabbitTemplate rabbitTemplate() {
		final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
		//rabbitTemplate.setMessageConverter(jsonMessageConverter());
		return rabbitTemplate;
	}

}
