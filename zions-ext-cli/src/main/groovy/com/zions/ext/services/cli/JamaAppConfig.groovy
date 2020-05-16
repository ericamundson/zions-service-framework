package com.zions.ext.services.cli

import com.zions.mr.services.rest.MrGenericRestClient
import com.zions.common.services.attachments.IAttachments
import com.zions.common.services.cache.CacheInterceptorService
import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cache.MongoDBCacheManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.restart.CheckpointManagementService
import com.zions.common.services.restart.ICheckpointManagementService
import com.zions.common.services.restart.IRestartManagementService
import com.zions.common.services.restart.RestartManagementService
import com.zions.vsts.services.attachments.AttachmentManagementService
import com.zions.jama.services.rest.JamaGenericRestClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

@Configuration
@Profile("jama")
@ComponentScan(["com.zions.jama.services","com.zions.vsts.services","com.zions.spock.services","com.zions.common.services.spock", "com.zions.common.services.restart", "com.zions.common.services.cacheaspect"])
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
	@Value('${cache.location}')
	String cacheLocation

	@Autowired
	@Value('${mr.url:}')
	String mrUrl

	@Autowired
	@Value('${tfs.userid:}')
	String tfsUserid

	@Bean
	JavaMailSender sender() {
		return new JavaMailSenderImpl()
	}

	@Value('${cache.type:file}')
	String cacheType

	@Bean 
	ICacheManagementService cacheManagementService() {
		return new CacheManagementService(cacheLocation)
	}
	
	@Bean
	CacheInterceptorService cacheInterceptorService() {
		return new CacheInterceptorService()
	}

	@Bean
	CommandManagementService commandManagementService() {
		return new CommandManagementService();
	}
	@Bean
	IGenericRestClient jamaGenericRestClient() {
		return new JamaGenericRestClient(jamaUrl, jamaUser, jamaPassword)
	}
	@Bean
	IGenericRestClient mrGenericRestClient() {
		return new MrGenericRestClient(mrUrl, tfsUserid)
	}

//	@Bean
//	IAttachments attachmentsService() {
//		return new AttachmentManagementService();
//	}


}