package com.zions.vsts.services.setcolor

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.work.templates.ProcessTemplateService
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
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration


@SpringBootApplication(exclude=[MongoAutoConfiguration,MongoDataAutoConfiguration,EmbeddedMongoAutoConfiguration,LdapAutoConfiguration])
public class SetColorApplication {

	public static void main(String[] args) {
		
 		SpringApplication app = new SpringApplication(SetColorApplication.class);
		app.run(args);
				

	}
}
