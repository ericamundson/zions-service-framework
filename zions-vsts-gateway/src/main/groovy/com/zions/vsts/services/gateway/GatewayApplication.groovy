package com.zions.vsts.services.gateway;

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

@SpringBootApplication
@Slf4j
public class GatewayApplication {
	
	@Autowired
	private Map<String, CliAction> actionsMap;
	
	static public void main(String[] args) {
		SpringApplication app = new SpringApplication(GatewayApplication.class);
		app.setBannerMode(Banner.Mode.OFF);
		
		app.run(args);
	}
	
	
}
