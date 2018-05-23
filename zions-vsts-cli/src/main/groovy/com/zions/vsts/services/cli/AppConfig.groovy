package com.zions.vsts.services.cli

import com.zions.common.services.cli.action.CliAction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("default")
@ComponentScan(["com.zions.vsts.services","com.zions.clm.services","com.zions.bb.services"])
public class AppConfig {
	Map<String, CliAction> actions;
	
	@Bean
	public Map<String, CliAction> actionsMap() {
		if (actions == null)
			actions = [:];
		return actions;
	}
}