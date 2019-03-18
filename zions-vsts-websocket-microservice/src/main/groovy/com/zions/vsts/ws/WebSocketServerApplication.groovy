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


/* Pull in HTML content from C:\Resources\ and deploy
 * to localhost using Tomcat on port 8080 * when running as Java Application
 * author: Michael Angelastro 2/25/19 */

@SpringBootApplication(exclude=[MongoAutoConfiguration,MongoDataAutoConfiguration])
public class WebSocketServerApplication {


	public static void main(String[] args) {
		
        /*Call AppConfigTest.class to define custom resource path*/
		
		SpringApplication app = new SpringApplication(WebSocketServerApplication.class);
		app.run(args);
				

	}
}
