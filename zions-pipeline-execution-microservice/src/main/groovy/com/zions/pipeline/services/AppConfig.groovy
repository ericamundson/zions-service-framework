package com.zions.pipeline.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.core.io.ClassPathResource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.attachments.AttachmentManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient
import com.zions.mr.services.rest.MrGenericRestClient
import com.zions.vsts.services.rmq.mixins.MessageFanoutConfigTrait
import com.zions.vsts.services.tfs.rest.MultiUserGenericRestClient

@Configuration
@ComponentScan(["com.zions.pipeline.services", "com.zions.vsts.services"])
public class AppConfig implements MessageFanoutConfigTrait {
	@Bean
	JavaMailSender sender() {
		return new JavaMailSenderImpl()
	}
	@Bean 
	ICacheManagementService cacheManagementService() {
		return new CacheManagementService(cacheLocation)
	}

	@Bean
	CommandManagementService commandManagementService() {
		return new CommandManagementService();
	}

	@Bean
	AttachmentManagementService attachmentManagementService() {
		return new AttachmentManagementService();
	}

	@Value('${tfs.url:}')
	String tfsUrl
	@Value('${tfs.user:}')
	String tfsUser
	@Value('${tfs.token:}')
	String tfsToken

	@Bean
	IGenericRestClient genericRestClient() {
		return new MultiUserGenericRestClient()
	}
	
	@Bean
	IGenericRestClient mrGenericRestClient() {
		return new MrGenericRestClient('', '')
	}


	@Autowired
	@Value('${cache.location:none}')
	String cacheLocation
	
}