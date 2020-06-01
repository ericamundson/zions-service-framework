package com.zions.jama.services.requirements

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.test.context.ContextConfiguration

import com.mongodb.MongoClient
import com.zions.common.services.attachments.IAttachments
import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cache.MongoDBCacheManagementService
import com.zions.common.services.cacheaspect.CacheInterceptor
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.mongo.EmbeddedMongoBuilder
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.SpockLabeler
import com.zions.jama.services.rest.JamaGenericRestClient
import com.zions.rm.services.requirements.ClmRequirementsModule
import groovy.util.logging.Slf4j
import spock.lang.Ignore
import spock.lang.Specification
import spock.mock.DetachedMockFactory
import com.mongodb.diagnostics.logging.Loggers;

import java.util.logging.Level;
import java.util.logging.Logger;

@ContextConfiguration(classes=[JamaRequirementsManagementServiceSpecConfig])
class JamaRequirementsManagementServiceIntegration extends Specification {
	
	@Autowired
	JamaRequirementsManagementService underTest
	
	@Autowired
	JamaRequirementsItemManagementService jamaRequirementsItemManagementService
	
	@Autowired
	ICacheManagementService cacheManagementService
	
	@Autowired
	IGenericRestClient jamaGenericRestClient
	
	@Ignore
	def 'Get attachment file content'() {
		given: 'Item ID'
		int itemId = 2375034
		
		when: 'retrieving attachment content'
		def resultData = underTest.getContent(itemId)
		
		then: 'attachment data was returned'
		resultData != null
	}
}

@TestConfiguration
@Profile("integration-test")
@ComponentScan(["com.zions.common.services.rest", "com.zions.common.services.cacheaspect", "com.zions.common.services.cache", "com.zions.ext.services", "com.zions.common.services.restart", "com.zions.common.services.cache.db", "com.zions.common.services.test","com.zions.rm.services.requirements.handlers"])
@PropertySource("classpath:integration-test.properties")
@EnableMongoRepositories(basePackages = "com.zions.common.services.cache.db")
public class JamaRequirementsManagementServiceSpecConfig {
	def factory = new DetachedMockFactory()

	@Value('${jama.url:https://zionsbancorp.jamacloud.com}')
	String jamaUrl
	
	@Autowired
	@Value('${jama.user}')
	String jamaUser
	
	@Autowired
	@Value('${jama.password}')
	String jamaPassword

	@Autowired
	@Value('${jama.projectid:none}')
	String jamaProjectID
	
	@Value('${mr.url:none}')
	String mrUrl
	
	@Value('${tfs.user:none}')
	String tfsUserid
	
	@Value('${cache.location}')
	String cacheLocation
		
	@Bean
	JamaRequirementsManagementService underTest() {
		return new JamaRequirementsManagementService()
	}
	
	@Bean
	JamaRequirementsItemManagementService clmRequirementsItemManagementService() {
		return new JamaRequirementsItemManagementService()
	}
	
	@Bean
	JamaRequirementsFileManagementService rmRequirementsFileManagementService()
	{
		return new JamaRequirementsFileManagementService()
	}
	
	@Bean
	ICacheManagementService cacheManagementService() {
		//return new CacheManagementService(cacheLocation)
		return new MongoDBCacheManagementService()
	}
	
	@Bean
	IGenericRestClient jamaGenericRestClient() {
		return new JamaGenericRestClient(jamaUrl, jamaUser, jamaPassword)
	}
		
	@Bean
	CommandManagementService commandManagementService() {
		return new CommandManagementService();
	}
	

	@Value('${spring.data.mongodb.host:utmsdev0598}')
	String dbHost

	@Value('${spring.data.mongodb.database:adomigration_dev}')
	String database

	@Bean
	MongoClient mongoClient() throws UnknownHostException {
		Logger.getLogger(Loggers.PREFIX).setLevel(Level.OFF);
		def builder = new EmbeddedMongoBuilder()
			.version('3.2.16')
			//.tempDir('mongodb')
			.installPath('../zions-common-data/mongodb/win32/mongodb-win32-x86_64-3.2.16/bin')
			.bindIp("localhost")
			.port(12345)
			.build();
		return builder
	}
	
	public @Bean MongoTemplate mongoTemplate() throws Exception {
		return new MongoTemplate(mongoClient(), database);
	}
}
