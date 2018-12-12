package com.zions.ext.services.cli

import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cli.action.CliAction

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

@Configuration
@Profile("clm")
@ComponentScan(["com.zions.vsts.services","com.zions.clm.services"])
public class CLMAppConfig {
	
	@Autowired
	@Value('${cache.location}')
	String cacheLocation

	@Bean
	JavaMailSender sender() {
		return new JavaMailSenderImpl()
	}
	
	@Bean 
	CacheManagementService cacheManagementService() {
		return new CacheManagementService(cacheLocation)
	}
}