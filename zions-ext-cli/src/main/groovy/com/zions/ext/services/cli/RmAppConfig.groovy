package com.zions.ext.services.cli

import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.mr.services.rest.MrGenericRestClient 
import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.rest.IGenericRestClient

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

@Configuration
@Profile("rm")
@ComponentScan(["com.zions.rm.services","com.zions.vsts.services","com.zions.ext.services"])
public class RmAppConfig {
	@Autowired
	@Value('${clm.url}') 
	String clmUrl

	@Autowired
	@Value('${mr.url}')
	String mrUrl
	
	@Autowired
	@Value('${clm.user}') 
	String userid 
	
	@Autowired
	@Value('${tfs.user}')
	String tfsUserid
	
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
	ICacheManagementService cacheManagementService() {
		return new CacheManagementService(cacheLocation)
	}
	
	@Bean
	IGenericRestClient rmGenericRestClient() {
		return new ClmGenericRestClient(clmUrl, userid, password)
	}
	
	@Bean
	IGenericRestClient mrGenericRestClient() {
		return new MrGenericRestClient(mrUrl, tfsUserid)
	}
	
	@Bean
	CommandManagementService commandManagementService() {
		return new CommandManagementService();
	}

}