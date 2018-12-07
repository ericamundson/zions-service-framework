package com.zions.ext.services.cli

import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.rest.IGenericRestClient
import com.zions.ext.services.cache.CacheManagementService
import com.zions.qm.services.test.ClmTestAttachmentManagementService

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

@Configuration
@Profile("qm")
@ComponentScan(["com.zions.qm.services","com.zions.vsts.services"])
public class QmAppConfig {
	@Autowired
	@Value('${clm.url}') 
	String clmUrl
	
	@Autowired
	@Value('${clm.user}') 
	String userid 
	
	@Autowired
	@Value('${clm.password}') 
	String password
	
	
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
	
	@Bean
	IGenericRestClient qmGenericRestClient() {
		return new ClmGenericRestClient(clmUrl, userid, password)
	}
}