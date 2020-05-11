package com.zions.vsts.services.rmq.mixins

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.AcknowledgeMode

trait MessageConfigTrait {
	@Value('${routing.exchange.name:}')
	String routingExchangeName

	@Value('${routing.direct.key:}')
	String routingDirectKey

	@Value('${queue.name:}')
	String queueName

	@Bean
	Queue queue() {
		return new Queue(queueName, true);
	}

	@Bean
	DirectExchange exchange() {
		return new DirectExchange(routingExchangeName);
	}

	@Bean
	Binding binding(Queue queue, DirectExchange exchange) {
		return BindingBuilder.bind(queue).to(exchange).with(routingDirectKey);
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
