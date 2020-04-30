package com.zions.vsts.services.policy;

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
//import org.springframework.boot.autoconfigure.data.ldap.LdapDataAutoConfiguration
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration

/**
 * Spring Boot application for the TFS branch policy service
 *
 */
@SpringBootApplication(exclude=[MongoAutoConfiguration,MongoDataAutoConfiguration,EmbeddedMongoAutoConfiguration,LdapAutoConfiguration])
public class PolicyApplication {

	public static void main(String[] args) {
		SpringApplication.run(PolicyApplication.class, args);
	}
}
