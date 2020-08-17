package com.zions.pipeline.services.cli

import com.mongodb.MongoClient
import com.mongodb.MongoClientOptions
import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.tfs.rest.GenericRestClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import com.zions.vsts.services.tfs.rest.MultiUserGenericRestClient

@Configuration
@Profile("default")
@ComponentScan(["com.zions.pipeline.services,com.zions.vsts.services,com.zions.xld.services,com.zions.xlr.services,com.zions.common.services.rest"])
@EnableMongoRepositories(basePackages = "com.zions.xlr.services.events.db")
public class AppConfig {
	Map<String, CliAction> actions;
	
	@Bean
	public Map<String, CliAction> actionsMap() {
		if (actions == null)
			actions = [:];
		return actions;
	}

	@Autowired
	@Value('${cache.location:cache}')
	String cacheLocation
	
	@Value('${tfs.url:}')
	String tfsUrl
	@Value('${tfs.user:}')
	String tfsUser
	@Value('${tfs.token:}')
	String tfsToken

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
	IGenericRestClient genericRestClient() {
		return new MultiUserGenericRestClient()
	}
	
	@Bean
	IGenericRestClient mrGenericRestClient() {
		return new GenericRestClient(tfsUrl, tfsUser, tfsToken)
	}
	
	@Value('${spring.data.mongodb.host:utmsdev0598}')
	String dbHost

	@Value('${spring.data.mongodb.database:xlrevents_dev}')
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