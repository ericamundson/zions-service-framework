package com.zions.vsts.services


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
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

import com.mongodb.MongoClient
import com.mongodb.MongoClientOptions
import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.mr.services.rest.MrGenericRestClient
import com.zions.vsts.services.attachments.AttachmentManagementService
import com.zions.vsts.services.tfs.rest.MultiUserGenericRestClient
import com.zions.vsts.services.rmq.mixins.MessageFanoutGenericConfigTrait




/* Will set default configs for ContentApplication */

@Configuration
@ComponentScan(["com.zions.vsts.services","com.zions.xlr.services","com.zions.common.services.logging"])
@EnableMongoRepositories(basePackages = "com.zions.xlr.services.events.db")
@Profile('test')
public class AppConfigTest implements MessageFanoutGenericConfigTrait {
	
	
//	@Bean
//	JavaMailSender sender() {
//		return new JavaMailSenderImpl()
//	}

	@Bean
	ICacheManagementService cacheManagementService() {
		return new CacheManagementService(cacheLocation)
	}

	@Bean
	CommandManagementService commandManagementService() {
		return new CommandManagementService();
	}

//	@Bean
//	AttachmentManagementService attachmentManagementService() {
//		return new AttachmentManagementService();
//	}
	
	@Bean
	IGenericRestClient genericRestClient() {
		return new MultiUserGenericRestClient()
	}

	@Bean
	IGenericRestClient mrGenericRestClient() {
		return new MrGenericRestClient('', '')
	}


	@Autowired
	@Value('${cache.location:cache}')
	String cacheLocation
	
	@Value('${spring.data.mongodb.host:utmsdev0598}')
	String dbHost

	@Value('${spring.data.mongodb.database:xlrevents}')
	String database
	
	@Bean
	public MongoClientOptions mongoOptions() {
		return MongoClientOptions.builder().maxConnectionIdleTime(1000 * 60 * 8).socketTimeout(30000).build();
	}

	@Bean
	MongoClient mongoClient() throws UnknownHostException {
		return new MongoClient(dbHost, mongoOptions());
	}
	
	public @Bean MongoTemplate mongoTemplate() throws Exception {
		return new MongoTemplate(mongoClient(), database);
	}


}


