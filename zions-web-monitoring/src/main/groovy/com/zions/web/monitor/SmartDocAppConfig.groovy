package com.zions.web.monitor

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

import com.zions.common.services.attachments.IAttachments
import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.mr.services.rest.MrGenericRestClient
import com.zions.vsts.services.asset.SharedAssetService
import com.zions.vsts.services.attachments.AttachmentManagementService
import com.zions.vsts.services.notification.NotificationService
import com.zions.vsts.services.test.TestManagementService
import com.zions.vsts.services.tfs.rest.MultiUserGenericRestClient



/* Will set default configs for ContentApplication */

@Configuration
@Profile("smartdoc")
@ComponentScan(["com.zions.mr.monitor","com.zions.vsts.services.work","com.zions.vsts.services.admin","com.zions.common.services.logging","com.zions.vsts.services.notification"])
public class SmartDocAppConfig {
	@Bean
	ICacheManagementService cacheManagementService() {
		return new CacheManagementService('na')
	}
	
	@Bean
	IGenericRestClient genericRestClient() {
		return new MultiUserGenericRestClient()
	}

	@Bean
	IAttachments attachmentService() {
		return new AttachmentManagementService();
	}
	
	@Bean
	TestManagementService testService() {
		return new TestManagementService();
	}
}
