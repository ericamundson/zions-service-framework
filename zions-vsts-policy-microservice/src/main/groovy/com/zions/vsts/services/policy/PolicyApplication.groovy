package com.zions.vsts.services.policy;

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * Spring Boot application for the TFS branch policy service
 *
 */
@SpringBootApplication
public class PolicyApplication {

	public static void main(String[] args) {
		SpringApplication.run(PolicyApplication.class, args);
	}
}
