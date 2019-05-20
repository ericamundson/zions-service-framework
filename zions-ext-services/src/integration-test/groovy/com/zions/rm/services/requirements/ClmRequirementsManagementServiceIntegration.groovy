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
import org.springframework.context.annotation.PropertySource
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
import com.zions.common.services.cacheaspect.CacheInterceptor
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.db.DatabaseQueryService
import com.zions.common.services.db.IDatabaseQueryService
import com.zions.common.services.mongo.EmbeddedMongoBuilder
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.DataGenerationService

import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[ClmRequirementsManagementServiceSpecConfig])
class ClmRequirementsManagementServiceIntegration extends Specification {
	
	@Autowired
	DataGenerationService dataGenerationService
	
	@Autowired
	ClmRequirementsManagementService underTest
	
	@Autowired
	ICacheManagementService cacheManagementService
	
	
	def 'Handle base requirement artifacts'() {
		given: 'A set of requirement artifacts'
		underTest.queryDatawarehouseSource(_) >> {
			
		}
				
		when: 'produce artifact data for text or non-text types'
		
		then: 'processing has no failures and verify set of text vs non-text artifact data elements count'
		
	}
	
	def 'Handle module requirement artifacts'() {
		given: 'A set of module artifacts'
	}
	

	def 'Get all links for artifact'() {
		setup: 'get artifact'
		def artifact = dataGenerationService.generate('/testdata/requirementWithLinks.xml')
		
		when: 'determine all requirement artifact links'
		def links = underTest.getAllLinks('7543', new Date(), artifact.artifact)
		
		then: 'validate number of links are 7'
		links.size() == 7
	}
	
	def 'Partial flush of query pages'() {
		given: 'A valid data warehouse query'
		
		when: 'Run a partial flush of two pages of requirements artifact query'
		boolean success = true
		try {
			underTest.flushQueries(2)
		} catch (e) {
			e.printStackTrace()
			success = false
		}
		def pages = cacheManagementService.getAllOfType('DataWarehouseQueryData')
		
		then: 'Validate no errors and there are two pages of requirements artifacts'
		success && pages.size() == 2
		
		cleanup:
		cacheManagementService.clear()

	}

}

@TestConfiguration
@Profile("integration-test")
@ComponentScan(["com.zions.common.services.cacheaspect", "com.zions.ext.services", "com.zions.common.services.restart", "com.zions.common.services.cache.db", "com.zions.common.services.test"])
@PropertySource("classpath:integration-test.properties")
@EnableMongoRepositories(basePackages = "com.zions.common.services.cache.db")
public class ClmRequirementsManagementServiceSpecConfig {
	def factory = new DetachedMockFactory()

	@Value('${clm.url:https://clm.cs.zionsbank.com}')
	String clmUrl
	
	@Value('${clm.user:none}')
	String clmUser
	
	@Value('${clm.password:none}')
	String clmPassword
	
	@Value('${mr.url:none}')
	String mrUrl
	
	@Value('${clm.user:none}')
	String userid
	
	@Value('${tfs.user:none}')
	String tfsUserid
	
	@Value('${clm.password:none}')
	String password
	
	@Value('${cache.location}')
	String cacheLocation
	
	@Value('${sql.resource.name}')
	String sqlResourceName
	
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
	
	@Bean
	IDatabaseQueryService databaseQueryService() {
		return factory.Spy(DatabaseQueryService)
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
