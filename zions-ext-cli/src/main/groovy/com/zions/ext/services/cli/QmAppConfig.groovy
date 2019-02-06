package com.zions.ext.services.cli

import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.common.services.attachments.IAttachments
import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.ext.services.cli.action.rest.RestClient
import com.zions.mr.services.rest.MrGenericRestClient
import com.zions.qm.services.test.ClmTestAttachmentManagementService
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
	ICacheManagementService cacheManagementService() {
		return new CacheManagementService(cacheLocation)
	}
	
	@Bean
	IGenericRestClient qmGenericRestClient() {
		return new ClmGenericRestClient(clmUrl, userid, password)
	}
	
	@Bean
	IGenericRestClient mrGenericRestClient() {
		return new MrGenericRestClient('none', 'none')
	}

	@Bean
	CliAction restClient() {
		return new RestClient()
	}
	
	
	@Bean
	CommandManagementService commandManagementService() {
		return new CommandManagementService();
	}
	

//	@Bean
//	IAttachments attachmentsService() {
//		return new AttachmentManagementService();
//	}

}