package com.zions.ext.services.cli

import com.zions.common.services.attachments.IAttachments
import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.restart.CheckpointManagementService
import com.zions.common.services.restart.ICheckpointManagementService
import com.zions.common.services.restart.IRestartManagementService
import com.zions.common.services.restart.RestartManagementService
import com.zions.mr.services.rest.MrGenericRestClient
import com.zions.vsts.services.attachments.AttachmentManagementService
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
@ComponentScan(["com.zions.vsts.services","com.zions.clm.services", "com.zions.common.services.restart"])
public class CLMAppConfig {
	
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
	CommandManagementService commandManagementService() {
		return new CommandManagementService();
	}
	
	@Bean
	IGenericRestClient mrGenericRestClient() {
		return new MrGenericRestClient('', '')
	}
	
//	@Bean
//	IAttachments attachmentsService() {
//		return new AttachmentManagementService();
//	}

}