package com.zions.vsts.services.policy

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.core.io.ClassPathResource
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.attachments.AttachmentManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient

@Configuration
@ComponentScan("com.zions.vsts.services")
public class AppConfig {
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
		return new GenericRestClient(tfsUrl, tfsUser, tfsToken)
	}

	@Autowired
	@Value('${cache.location}')
	String cacheLocation
	
}