package com.zions.ext.services.cli

import com.zions.common.services.cli.action.CliAction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("bb")
@ComponentScan(["com.zions.vsts.services","com.zions.bb.services","com.zions.ext.services"])
public class BBAppConfig {
	Map<String, CliAction> actions;
	
	@Bean
	public Map<String, CliAction> actionsMap() {
		if (actions == null)
			actions = [:];
		return actions;
	}
}