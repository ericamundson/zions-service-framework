package com.zions.webbot.cli

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
import org.springframework.context.annotation.PropertySource
import com.zions.common.services.attachments.IAttachments
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.notification.NotificationService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.mr.services.rest.MrGenericRestClient
import com.zions.vsts.services.asset.SharedAssetService
import com.zions.vsts.services.tfs.rest.MultiUserGenericRestClient



/* Will set default configs for ContentApplication */

@Configuration
@Profile("setpassword")
@PropertySource("classpath:pwapplication.properties")
@ComponentScan(["com.zions.sa.passwords","com.zions.auto","com.zions.vsts.services.asset","com.zions.vsts.services.admin","com.zions.common.services.logging","com.zions.common.services.notification"])
public class SetPasswordAppConfig {

	
	@Bean
	IGenericRestClient genericRestClient() {
		return new MultiUserGenericRestClient()
	}


}
