package com.zions.rm.services.requirements

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.test.context.ContextConfiguration

import com.mongodb.MongoClient
import com.zions.clm.services.rest.ClmBGenericRestClient
import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.common.services.attachments.IAttachments
import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cache.MongoDBCacheManagementService
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.mongo.EmbeddedMongoBuilder
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.DataGenerationService

import spock.lang.Specification

@ContextConfiguration(classes=[ClmRequirementsManagementServiceSpecConfig])
class ClmRequirementsManagementServiceSpec extends Specification {
	
	@Autowired
	DataGenerationService dataGenerationService
	
	@Autowired
	ClmRequirementsManagementService underTest

	def 'Get all links for artifact'() {
		setup: 'get artifact'
		def artifact = dataGenerationService.generate('/testdata/requirementWithLinks.xml')
		
		when: 'run all links'
		def links = underTest.getAllLinks('7543', new Date(), artifact.artifact)
		
		then:
		true
	}

}

@TestConfiguration
@Profile("test")
@ComponentScan(["com.zions.common.services.cacheaspect", "com.zions.vsts.services","com.zions.ext.services", "com.zions.common.services.restart", "com.zions.common.services.cache.db", "com.zions.common.services.test"])
@EnableMongoRepositories(basePackages = "com.zions.common.services.cache.db")
public class ClmRequirementsManagementServiceSpecConfig {
	@Autowired
	@Value('${clm.url:https://clm.cs.zionsbank.com}')
	String clmUrl
	
	@Autowired
	@Value('${clm.user:none}')
	String clmUser
	
	@Autowired
	@Value('${clm.password:none}')
	String clmPassword
	
	@Autowired
	@Value('${mr.url:none}')
	String mrUrl
	
	@Autowired
	@Value('${clm.user:none}')
	String userid
	
	@Autowired
	@Value('${tfs.user:none}')
	String tfsUserid
	
	@Autowired
	@Value('${clm.password:none}')
	String password
	
	@Autowired
	@Value('${cache.location}')
	String cacheLocation
	
	@Bean
	ClmRequirementsManagementService underTest() {
		return new ClmRequirementsManagementService()
	}

	
	@Bean
	ICacheManagementService cacheManagementService() {
		//return new CacheManagementService(cacheLocation)
		return new MongoDBCacheManagementService()
	}
	
	@Bean
	IGenericRestClient rmGenericRestClient() {
		return new ClmGenericRestClient(clmUrl, userid, password)
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
	DataGenerationService dataGenerationService() {
		return new DataGenerationService()
	}


	@Value('${spring.data.mongodb.host:utmsdev0598}')
	String dbHost

	@Value('${spring.data.mongodb.database:adomigration_dev}')
	String database

	@Bean
	MongoClient mongoClient() throws UnknownHostException {
		return new EmbeddedMongoBuilder()
			.version('3.2.16')
			//.tempDir('mongodb')
			.installPath('../zions-common-data/mongodb/win32/mongodb-win32-x86_64-3.2.16/bin')
			.bindIp("localhost")
			.port(12345)
			.build();
	}
	
	public @Bean MongoTemplate mongoTemplate() throws Exception {
		return new MongoTemplate(mongoClient(), database);
	}
}
