package com.zions.xlr.services.publish


import org.apache.catalina.connector.Connector
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.boot.web.servlet.server.ServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

import groovy.util.logging.Slf4j

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import java.net.InetAddress

import com.zions.vsts.services.rmq.mixins.MessageSenderFanoutTrait


/**
 *  Will set default configs for ContentApplication 
 */
@Configuration
@ComponentScan(["com.zions.xlr.services.publish","com.zions.xlr.services.query"])
//@PropertySource("classpath:eventpublisher.properties")
//@EnableWebSocketMessageBroker
@Slf4j
public class AppConfig {
//	@Value('${spring.rabbitmq.host:localhost}')
//	String rabbitmqHost
	
	@Value('${spring.rabbitmq.host:localhost}')
	String host;
	@Value('${spring.rabbitmq.port:30672}')
	int port;
	@Value('${spring.rabbitmq.username:guest}')
	String username;
	@Value('${spring.rabbitmq.password:guest}')
	String password;
	

	CachingConnectionFactory connectionFactory() {
		CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(host, port);
		cachingConnectionFactory.setUsername(username);
		cachingConnectionFactory.setPassword(password);
		return cachingConnectionFactory;
	}
	

	@Bean
	@Qualifier('rabbitTemplate')
	public RabbitTemplate rabbitTemplate() {
		final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
		//rabbitTemplate.setMessageConverter(jsonMessageConverter());
		return rabbitTemplate;
	}

	

}


