package com.zions.ext.services.cli

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

import com.zions.common.services.cli.action.CliAction

@Configuration
@ComponentScan(["com.zions.ext.services","com.zions.clm.services"])
public class AppConfig {
	Map<String, CliAction> actions;
	
	@Bean
	public Map<String, CliAction> actionsMap() {
		if (actions == null)
			actions = [:];
		return actions;
	}
}