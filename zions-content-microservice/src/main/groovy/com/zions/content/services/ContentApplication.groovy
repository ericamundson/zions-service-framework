package com.zions.content.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

import com.zions.common.services.cli.action.CliAction

@Configuration
@ComponentScan("com.zions.vsts.services")
public class ContentApplication {
	Map<String, CliAction> actions;
	
	@Bean
	public Map<String, CliAction> actionsMap() {
		if (actions == null)
			actions = [:];
		return actions;
	}
}