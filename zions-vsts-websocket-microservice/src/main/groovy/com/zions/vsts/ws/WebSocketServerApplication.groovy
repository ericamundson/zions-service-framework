package com.zions.vsts.ws;


import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.Banner
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration


/**
 * WebSocket micro-service main class.
 * 
 * @author z091182
 *
 */
@SpringBootApplication(exclude=[MongoAutoConfiguration,MongoDataAutoConfiguration])
public class WebSocketServerApplication {


	public static void main(String[] args) {
		
		SpringApplication app = new SpringApplication(WebSocketServerApplication.class);
		app.run(args);
				

	}
}
