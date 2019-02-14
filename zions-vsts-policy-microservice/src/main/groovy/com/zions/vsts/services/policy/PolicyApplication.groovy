package com.zions.vsts.services.policy;

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration

/**
 * Spring Boot application for the TFS branch policy service
 *
 */
@SpringBootApplication(exclude=[MongoAutoConfiguration,MongoDataAutoConfiguration])
public class PolicyApplication {

	public static void main(String[] args) {
		SpringApplication.run(PolicyApplication.class, args);
	}
}
