package com.zions.vsts.services.setcolor


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter

import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.mr.services.rest.MrGenericRestClient
import com.zions.vsts.services.asset.SharedAssetService
import com.zions.vsts.services.attachments.AttachmentManagementService
import com.zions.vsts.services.tfs.rest.MultiUserGenericRestClient



/* Will set default configs for ContentApplication */

@Configuration
@ComponentScan(["com.zions.vsts.services.work","com.zions.vsts.services.admin","com.zions.common.services.logging"])
public class AppConfig  {
	
	
	@Bean
	ICacheManagementService cacheManagementService() {
		return new CacheManagementService(cacheLocation)
	}
	
	@Bean
	IGenericRestClient genericRestClient() {
		return new MultiUserGenericRestClient()
	}
	
	@Bean
	SharedAssetService sharedAssetService() {
		return new SharedAssetService()
	}

	@Autowired
	@Value('${tfs.colorMapUID:}')
	String colorMapUID
}


