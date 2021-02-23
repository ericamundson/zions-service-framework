package com.zions.pipeline.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.core.io.ClassPathResource
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.command.CommandManagementService
import com.mongodb.MongoClient
import com.mongodb.MongoClientOptions
import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.attachments.AttachmentManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient
import com.zions.mr.services.rest.MrGenericRestClient
import com.zions.vsts.services.rmq.mixins.MessageFanoutConfigTrait
import com.zions.vsts.services.tfs.rest.MultiUserGenericRestClient

@Configuration
@ComponentScan(["com.zions.pipeline.services", "com.zions.vsts.services"])
@EnableMongoRepositories(basePackages = "com.zions.pipeline.services.db")
@Profile('dev')
public class AppConfigDev implements MessageFanoutConfigTrait {
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

	@Value('${tfs.url:}')
	String tfsUrl
	@Value('${tfs.user:}')
	String tfsUser
	@Value('${tfs.token:}')
	String tfsToken

	@Bean
	IGenericRestClient genericRestClient() {
		return new MultiUserGenericRestClient()
	}
	
	@Bean
	IGenericRestClient mrGenericRestClient() {
		return new MrGenericRestClient('', '')
	}


	@Autowired
	@Value('${cache.location:none}')
	String cacheLocation
	
	@Value('${spring.data.mongodb.host:utmsdev0598}')
	String dbHost

	@Value('${spring.data.mongodb.database:pipelines_test}')
	String database
	
	@Bean
	public MongoClientOptions mongoOptions() {
		return MongoClientOptions.builder().maxConnectionIdleTime(1000 * 60 * 8).socketTimeout(30000).build();
	}

	@Bean
	MongoClient mongoClient() throws UnknownHostException {
		MongoClient client = new MongoClient(dbHost, mongoOptions());
		
		return client
	}
	
	public @Bean MongoTemplate mongoTemplate() throws Exception {
		MongoTemplate template = new MongoTemplate(mongoClient(), database);
		return template
	}

	
}