package com.zions.ext.services.cli

import com.zions.common.services.cli.action.CliAction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

@Configuration
@Profile("rm")
@ComponentScan(["com.zions.rm.services","com.zions.vsts.services"])
public class RmAppConfig {
	@Bean
	JavaMailSender sender() {
		return new JavaMailSenderImpl()
	}
}