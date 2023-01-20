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

trait MessageK8sConfigTrait {

	@Value('${spring.k8srabbitmq.host:localhost}')
	String k8shost;
	@Value('${spring.k8srabbitmq.port:30672}')
	int k8sport;
	@Value('${spring.k8srabbitmq.username:}')
	String k8sUsername;
	@Value('${spring.k8srabbitmq.password:}')
	String k8sPassword;
	
	@Value('${spring.rabbitmq.username:}')
	String username;
	@Value('${spring.rabbitmq.password:}')
	String password;
	
	@Value('${spring.rabbitmq.host:localhost}')
	String host;

	CachingConnectionFactory k8sConnectionFactory() {
		CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(k8shost, k8sport);
		cachingConnectionFactory.setUsername(k8sUsername);
		cachingConnectionFactory.setPassword(k8sPassword);
		return cachingConnectionFactory;
	}
	
//	CachingConnectionFactory connectionFactory() {
//		CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(host);
//		cachingConnectionFactory.setUsername(username);
//		cachingConnectionFactory.setPassword(password);
//		return cachingConnectionFactory;
//	}

	

	@Bean
	@Qualifier('k8sRabbitTemplate')
	public RabbitTemplate k8sRabbitTemplate() {
		final RabbitTemplate rabbitTemplate = new RabbitTemplate(k8sConnectionFactory());
		//rabbitTemplate.setMessageConverter(jsonMessageConverter());
		return rabbitTemplate;
	}
	
	//@Primary
//	@Bean
//	@Qualifier('rabbitTemplate')
//	public RabbitTemplate rabbitTemplate() {
//		final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
//		//rabbitTemplate.setMessageConverter(jsonMessageConverter());
//		return rabbitTemplate;
//	}

}
