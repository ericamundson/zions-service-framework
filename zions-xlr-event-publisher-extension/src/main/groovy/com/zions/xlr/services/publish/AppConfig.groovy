package com.zions.xlr.services.publish


import org.apache.catalina.connector.Connector
import org.springframework.beans.factory.annotation.Autowired
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


/**
 *  Will set default configs for ContentApplication 
 */
@Configuration
@ComponentScan(["com.zions.xlr.services.publish","com.zions.xlr.services.query"])
@PropertySource("classpath:eventpublisher.properties")
//@EnableWebSocketMessageBroker
@Slf4j
public class AppConfig  {
//	@Value('${spring.rabbitmq.host:localhost}')
//	String rabbitmqHost
	
	@Value('${xlr.hosts:}')
	String[] xlrHosts
	
	@Value('#{${rabbit.hosts.map}}')
	Map<String,String> rabbitHostMap
	
	@Bean
	RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
		RabbitTemplate rt = new RabbitTemplate(connectionFactory)
		return rt
	}
	
	@Bean
	ConnectionFactory connectionFactory() {
		String xlHost = InetAddress.getLocalHost().getHostName().toLowerCase()
		String rabbitmqHost = rabbitHostMap[xlHost]
		if (!rabbitmqHost) {
			rabbitmqHost = 'utmvti0192'
		}
		return new CachingConnectionFactory(rabbitmqHost)
	}
	
	

}


