package com.zions.vsts.services.build


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.command.CommandManagementService
import com.zions.vsts.services.attachments.AttachmentManagementService



/* Will set default configs for ContentApplication */

@Configuration
@ComponentScan("com.zions.vsts.services")
public class AppConfig  {
	
	
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

	@Autowired
	@Value('${cache.location:cache}')
	String cacheLocation

}


