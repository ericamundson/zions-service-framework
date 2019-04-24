package com.zions.common.services.cache;


import static org.junit.Assert.*
import groovy.json.JsonSlurper
import com.mongodb.Mongo
import com.mongodb.MongoClient
import com.zions.common.services.cache.db.CacheItemRepository
import com.zions.common.services.mongo.EmbeddedMongoBuilder
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.DataGenerationService

import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.data.mongodb.config.AbstractMongoConfiguration
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.test.context.ContextConfiguration

import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[MongoDBCacheManagementServiceTestConfig])
public class MongoDBCacheManagementServiceSpec extends Specification {

	@Autowired
	ICacheManagementService underTest
	
	@Autowired
	DataGenerationService dataGenerationService

	def 'saveBinaryAsAttachment for project name success flow.'(){

		def result= new ByteArrayInputStream();

		when: 'calling of method under test (saveBinaryAsAttachment)'
		def keyname = underTest.saveBinaryAsAttachment( result ,'','')
		// 218-Test Plan
		then: ''
		true

	}

	@Test
	def 'saveToCache for project name success flow.'(){

		def data = dataGenerationService.generate('/testdata/TestPlanT_Cache.json')

		when: 'calling of method under test (data)'
		def keyname = underTest.saveToCache( data ,'1',ICacheManagementService.PLAN_DATA)
		def testplan = underTest.getFromCache( '1',ICacheManagementService.PLAN_DATA)

		then: ''
		testplan != null
	}

	@Test
	def 'getFromCache for project name success flow.'(){

		def data = dataGenerationService.generate('/testdata/TestPlanT_Cache.json')

		when: 'calling of method under test (getFromCache)'
		def keyname = underTest.getFromCache( '1',ICacheManagementService.PLAN_DATA)

		then: ''
		true
	}
}

@TestConfiguration
@Profile("test")
@ComponentScan(["com.zions.common.services.test", "com.zions.common.services.cache.db"])
@PropertySource("classpath:test.properties")
@EnableMongoRepositories(basePackages = "com.zions.common.services.cache.db")
class MongoDBCacheManagementServiceTestConfig {
	def factory = new DetachedMockFactory()

	@Autowired
	@Value('${cache.location:}')
	String cacheLocation

	
	@Bean
	DataGenerationService dataGenerationService() {
		return new DataGenerationService()
	}
	
	public MongoClient mongoClient() throws Exception {
		
		return new EmbeddedMongoBuilder()
			.version('3.2.16')
			.tempDir('build/tmp')
			.downloadPath('file:./../zions-common-data/mongodb/')
			.artifactStorePath('./build/embeddeddb')
			.bindIp("127.0.0.1")
			.port(12345)
			.build();
	}

	
	public @Bean MongoTemplate mongoTemplate() throws Exception {
		return new MongoTemplate(mongoClient(), getDatabaseName());
	}

	protected String getDatabaseName() {
		// TODO Auto-generated method stub
		return 'coredev';
	}
	
	@Bean
	ICacheManagementService underTest() {
		return new MongoDBCacheManagementService()
	}

}
