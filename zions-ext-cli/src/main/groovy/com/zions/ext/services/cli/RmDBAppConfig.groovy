package com.zions.ext.services.cli

import com.mongodb.MongoClient
import com.zions.clm.services.rest.ClmBGenericRestClient
import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.mr.services.rest.MrGenericRestClient
import com.zions.common.services.attachments.IAttachments
import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cache.MongoDBCacheManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.attachments.AttachmentManagementService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

@Configuration
@Profile("rmdb")
@ComponentScan(["com.zions.rm.services","com.zions.vsts.services","com.zions.ext.services", "com.zions.common.services.restart", "com.zions.common.services.cache.db", "com.zions.common.services.cacheaspect"])
@EnableMongoRepositories(basePackages = "com.zions.common.services.cache.db")
public class RmDBAppConfig {
	@Autowired
	@Value('${clm.url}') 
	String clmUrl
	
	@Autowired
	@Value('${clm.user}')
	String clmUser
	
	@Autowired
	@Value('${clm.password}')
	String clmPassword
	
	@Autowired
	@Value('${mr.url}')
	String mrUrl
	
	@Autowired
	@Value('${clm.user}') 
	String userid 
	
	@Autowired
	@Value('${tfs.user}')
	String tfsUserid
	
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
		return new MongoDBCacheManagementService()
	}
	
	@Bean
	IGenericRestClient rmGenericRestClient() {
		return new ClmGenericRestClient(clmUrl, userid, password)
	}
	
	@Bean
	IGenericRestClient mrGenericRestClient() {
		return new MrGenericRestClient(mrUrl, tfsUserid)
	}
	
	@Bean
	IGenericRestClient rmBGenericRestClient() {
		return new ClmBGenericRestClient(clmUrl, clmUser, clmPassword)
	}
	
	@Bean
	CommandManagementService commandManagementService() {
		return new CommandManagementService();
	}
	
	@Bean
	IAttachments attachmentService() {
		return new AttachmentManagementService();
	}

	@Value('${spring.data.mongodb.host:utmsdev0598}')
	String dbHost

	@Value('${spring.data.mongodb.database:adomigration_dev}')
	String database

	@Bean
	MongoClient mongoClient() throws UnknownHostException {
		return new MongoClient(dbHost);
	}
	
	public @Bean MongoTemplate mongoTemplate() throws Exception {
		return new MongoTemplate(mongoClient(), database);
	}
}