package com.zions.ext.services.cli

import com.zions.jama.services.rest.JamaGenericRestClient
import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.mr.services.rest.MrGenericRestClient
import com.zions.common.services.attachments.IAttachments
import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.rm.services.requirements.ClmRequirementsFileManagementService
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
@Profile("jama")
@ComponentScan(["com.zions.rm.services","com.zions.vsts.services","com.zions.ext.services", "com.zions.common.services.restart", "com.zions.common.services.cacheaspect", "com.zions.common.services.db"])
public class JamaAppConfig {
	@Autowired
	@Value('${jama.url}') 
	String jamaUrl
	
	@Autowired
	@Value('${jama.user}')
	String jamaUser
	
	@Autowired
	@Value('${jama.password}')
	String jamaPassword
	
	@Autowired
	@Value('${jama.user}') 
	String userid 
	
	@Autowired
	@Value('${tfs.user}')
	String tfsUserid
	
	@Autowired
	@Value('${jama.password}') 
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
	IGenericRestClient jamaGenericRestClient() {
		return new JamaGenericRestClient(jamaUrl, userid, password)
	}
	
	@Bean
	CommandManagementService commandManagementService() {
		return new CommandManagementService();
	}
	
	@Bean
	IAttachments attachmentService() {
		return new AttachmentManagementService();
	}
}