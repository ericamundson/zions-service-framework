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

trait MessageFanoutGenericConfigTrait {

	@Value('${generic.topics:}')
	String[] genericTopics

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

//	@Bean
//	@Qualifier('primaryQueue')
//	Queue primaryQueue() {
//		Queue queue = new Queue(queueName, queueDurable, queueExclusive, queueAutoDelete, ['x-dead-letter-exchange': '', 'x-dead-letter-routing-key': "${queueName}-dead"]);
//		
//		return queue
//	}
//	
//	@Bean
//	@Qualifier('deadLetterQueue')
//	public Queue deadLetterQueue() {
//		return new Queue("${queueName}-dead");
//	}
		


	@Bean
	Declarables declarables() {
		Declarables dec = new Declarables()
		DirectExchange dle = new DirectExchange('dead-letter-exchange', exchangeDurable, false)
		dec.declarables.add(dle)
		DirectExchange delaye = new DirectExchange('delay-exchange', exchangeDurable, false)
		delaye.setDelayed(true)
		dec.declarables.add(delaye)
		Queue parked = null	
		if (queueAutoDelete) {
			parked = QueueBuilder.durable('parked-queue')
					.autoDelete()
		            //.deadLetterExchange('dead-letter-exchange')
		            //.deadLetterRoutingKey("${queueName}")
					//.ttl(1000)
		            .build();
		} else {
			parked = QueueBuilder.durable('parked-queue')
					//.deadLetterExchange('dead-letter-exchange')
					//.deadLetterRoutingKey("${queueName}")
					//.ttl(1000)
					.build();

		}
		dec.declarables.add(parked)
		Queue notify = null	
		if (queueAutoDelete) {
			notify = QueueBuilder.durable('notification-queue')
					.autoDelete()
		            //.deadLetterExchange('dead-letter-exchange')
		            //.deadLetterRoutingKey("${queueName}")
					//.ttl(1000)
		            .build();
		} else {
			notify = QueueBuilder.durable('notification-queue')
					//.deadLetterExchange('dead-letter-exchange')
					//.deadLetterRoutingKey("${queueName}")
					//.ttl(1000)
					.build();

		}
		dec.declarables.add(notify)
		Queue prim = null 
		if (queueAutoDelete) {
			prim = QueueBuilder.durable(queueName)
					.autoDelete()
		            //.deadLetterExchange('dead-letter-exchange')
		            //.deadLetterRoutingKey("${queueName}-dead")
		            .build();
		} else {
			prim = QueueBuilder.durable(queueName)
					//.deadLetterExchange('dead-letter-exchange')
					//.deadLetterRoutingKey("${queueName}-dead")
					.build();

		}
		dec.declarables.add(prim)
		genericTopics.each { topic ->
			dec.declarables.add(new FanoutExchange(topic, exchangeDurable, false))
			Binding b = new Binding(queueName, Binding.DestinationType.QUEUE, topic, '', null)
			dec.declarables.add(b)
		}
		Binding b = new Binding("${queueName}", Binding.DestinationType.QUEUE, 'delay-exchange', "${queueName}", null)
		dec.declarables.add(b)
		b = new Binding('parked-queue', Binding.DestinationType.QUEUE, 'dead-letter-exchange', 'parked-queue', null)
		dec.declarables.add(b)
		b = new Binding('notification-queue', Binding.DestinationType.QUEUE, 'dead-letter-exchange', 'notification-queue', null)
		dec.declarables.add(b)
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
		container.setDefaultRequeueRejected(false);
		//container.setAcknowledgeMode(AcknowledgeMode.AUTO);
		return container;
	}

	@Bean
	MessageListenerAdapter listenerAdapter(MessageReceiverTrait receiver) {
		return new MessageListenerAdapter(receiver);
	}
	
	
}
