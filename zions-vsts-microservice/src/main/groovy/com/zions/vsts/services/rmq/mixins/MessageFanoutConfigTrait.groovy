package com.zions.vsts.services.rmq.mixins

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.AmqpAdmin
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.AcknowledgeMode

trait MessageFanoutConfigTrait {

	@Value('${ado.topics:}')
	String[] adoTopics

	@Value('${queue.name:}')
	String queueName
	
	@Value('${exchange.durable:true}')
	boolean exchangeDurable
	
	@Value('${queue.durable:true}')
	boolean queueDurable
	
	@Value('${queue.exclusive:false}')
	boolean queueExclusive
	
	@Value('${queue.autoDelete:false}')
	boolean queueAutoDelete

	@Bean
	Queue queue() {
		return new Queue(queueName, queueDurable, queueExclusive, queueAutoDelete);
	}

	@Bean
	Declarables exchanges() {
		Declarables dec = new Declarables()
		adoTopics.each { topic ->
			dec.declarables.add(new FanoutExchange(topic, exchangeDurable, false))
		}
		return dec;
	}

	@Bean
	Declarables binding(Queue queue, Declarables exchanges) {
		Declarables dec = new Declarables()
		exchanges.declarables.each { FanoutExchange exchange ->
			String exchangeName = exchange.name
			Binding b = new Binding(queueName, Binding.DestinationType.QUEUE, exchangeName, '', null)
			dec.declarables.add(b)
		}
		return dec;
	}
	
//	@Bean
//	public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
//		RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory)
//		return rabbitAdmin
//	}
	/**
	 * Required for executing adminstration functions against an AMQP Broker
	 */
	@Bean
	public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
		return new RabbitAdmin(connectionFactory);
	}

	@Bean
	SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
			MessageListenerAdapter listenerAdapter) {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.setQueueNames(queueName);
		container.setMessageListener(listenerAdapter);
		container.setAcknowledgeMode(AcknowledgeMode.AUTO);
		return container;
	}

	@Bean
	MessageListenerAdapter listenerAdapter(MessageReceiverTrait receiver) {
		return new MessageListenerAdapter(receiver, "receive");
	}
	
	
}
