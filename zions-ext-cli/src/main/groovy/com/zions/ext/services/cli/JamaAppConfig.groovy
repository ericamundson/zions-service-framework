package com.zions.ext.services.cli

import com.zions.mr.services.rest.MrGenericRestClient
import com.zions.vsts.services.tfs.rest.MultiUserGenericRestClient
import com.mongodb.MongoClient
import com.zions.common.services.attachments.IAttachments
import com.zions.common.services.cache.CacheInterceptorService
import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cache.MongoDBCacheManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.jama.services.rest.JamaGenericRestClient
import com.zions.jama.services.rest.JamaFormGenericRestClient
import com.zions.common.services.restart.CheckpointManagementService
import com.zions.common.services.restart.ICheckpointManagementService
import com.zions.common.services.restart.IRestartManagementService
import com.zions.common.services.restart.RestartManagementService
import com.zions.vsts.services.attachments.AttachmentManagementService
import com.zions.jama.services.rest.JamaGenericRestClient
import com.zions.vsts.services.mr.SmartDocManagementService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

@Configuration
@Profile("jama")
@ComponentScan(["com.zions.jama.services","com.zions.vsts.services.admin.member","com.zions.vsts.services.admin.project","com.zions.vsts.services.work","com.zions.spock.services","com.zions.common.services.spock", 
	"com.zions.common.services.restart", "com.zions.common.services.cache.db", "com.zions.common.services.cacheaspect",
	"com.zions.rm.services.requirements.ClmArtifact.groovy","com.zions.rm.services.requirements.ClmModuleElement.groovy","com.zions.rm.services.requirements.ClmRequirementsModule.groovy"])
@EnableMongoRepositories(basePackages = "com.zions.common.services.cache.db")
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
	
	@Autowired
	@Value('${tfs.users:}')
	String[] tfsUsers

	@Autowired
	@Value('${tfs.tokens:}')
	String[] tfsTokens

	@Bean
	JavaMailSender sender() {
		return new JavaMailSenderImpl()
	}

	@Value('${cache.type:file}')
	String cacheType

	
	@Bean 
	ICacheManagementService cacheManagementService() {
		return new MongoDBCacheManagementService()
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
	IGenericRestClient jamaFormGenericRestClient() {
		return new JamaFormGenericRestClient(jamaUrl, jamaUser, jamaPassword)
	}
	@Bean
	IGenericRestClient mrGenericRestClient() {
		return new MrGenericRestClient(mrUrl, tfsUserid)
	}
	@Bean
	IGenericRestClient genericRestClient() {
		return new MultiUserGenericRestClient()
	}

	@Bean
	SmartDocManagementService smartDoManagementService() {
		return new SmartDocManagementService(tfsUsers, tfsTokens)
	}
	@Bean
	IAttachments attachmentsService() {
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