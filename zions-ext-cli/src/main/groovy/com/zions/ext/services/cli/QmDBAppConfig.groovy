package com.zions.ext.services.cli

import com.mongodb.MongoClient
import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.common.services.attachments.IAttachments
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
import com.zions.ext.services.cli.action.rest.RestClient
import com.zions.mr.services.rest.MrGenericRestClient
import com.zions.qm.services.test.ClmTestAttachmentManagementService
import com.zions.vsts.services.attachments.AttachmentManagementService
import com.zions.vsts.services.tfs.rest.MultiUserGenericRestClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

@Configuration
@Profile("qmdb")
@ComponentScan(["com.zions.qm.services","com.zions.vsts.services", "com.zions.common.services.restart","com.zions.common.services.cache.db"])
@EnableMongoRepositories(basePackages = "com.zions.common.services.cache.db")
//@Import(QmAppConfig.class)
public class QmDBAppConfig {
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
	
	@Bean
	ICacheManagementService cacheManagementService() {
		return new MongoDBCacheManagementService()
	}
	
	@Bean
	IGenericRestClient genericRestClient() {
		return new MultiUserGenericRestClient()
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