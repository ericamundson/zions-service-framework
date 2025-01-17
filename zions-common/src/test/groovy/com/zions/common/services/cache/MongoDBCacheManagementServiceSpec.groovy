package com.zions.common.services.cache;


import groovy.json.JsonSlurper
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoClient
import com.mongodb.MongoClientSettings
import com.zions.common.services.cache.db.CacheItemRepository
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.DataGenerationService
import com.zions.common.services.test.SpockLabeler

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.test.context.ContextConfiguration

import spock.lang.Specification
import spock.mock.DetachedMockFactory
import spock.lang.Ignore
/*
@ContextConfiguration(classes=[MongoDBCacheManagementServiceTestConfig])
public class MongoDBCacheManagementServiceSpec extends Specification {

	@Autowired
	MongoDBCacheManagementService underTest
	
	@Autowired
	DataGenerationService dataGenerationService
	@Ignore
	def 'saveBinaryAsAttachment for project name success flow.'(){

		def result= new ByteArrayInputStream();

		when: 'calling of method under test (saveBinaryAsAttachment)'
		def keyname = underTest.saveBinaryAsAttachment( result ,'','')
		// 218-Test Plan
		then: 'No failure'
		true

	}

	@Ignore
	def 'saveToCache for project name success flow.'(){

		def data = dataGenerationService.generate('/testdata/TestPlanT_Cache.json')

		when: 'calling of method under test (data)'
		def keyname = underTest.saveToCache( data ,'1',ICacheManagementService.PLAN_DATA)
		def testplan = underTest.getFromCache( '1', 'CCM', ICacheManagementService.PLAN_DATA)

		then: 'testplan != null'
		testplan != null
	}

	@Ignore
	def 'getFromCache for project name success flow.'(){

		def data = dataGenerationService.generate('/testdata/TestPlanT_Cache.json')

		when: 'calling of method under test (getFromCache)'
		def keyname = underTest.getFromCache( '1',ICacheManagementService.PLAN_DATA)

		then: 'No exception'
		true
	}
	
	@Ignore
	def 'getAllOfType test paging'() {
		setup: 'Add 400 items into cache'
		for (int i = 0; i < 400; i++) {
			def data = dataGenerationService.generate('/testdata/TestPlanT_Cache.json')
			underTest.saveToCache(data, "${i}", ICacheManagementService.PLAN_DATA)
		}		
		when: 'call getAllByType with paging'
		boolean success = true
		int page = 0
		try {
			while (true) {
				def plans = underTest.getAllOfType(ICacheManagementService.PLAN_DATA, page)
				if (plans.size() == 0) break;
				page++
			}
		} catch (e) {
			success = false
		}		
		then: 'two pages of 200 items are returned'
		success && page == 2
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
		return MongoClients.create("mongodb://localhost:12345")

//		return new EmbeddedMongoBuilder()
//			.version('3.2.16')
//			//.tempDir('mongodb')
//			.installPath('../zions-common-data/mongodb/win32/mongodb-win32-x86_64-3.2.16/bin')
//			.bindIp("localhost")
//			.port(12345)
//			.build();
			
	}

	
	public @Bean MongoTemplate mongoTemplate() throws Exception {
		return new MongoTemplate(mongoClient(), getDatabaseName());
	}

	protected String getDatabaseName() {
		
		return 'coredev';
	}
	
	@Bean
	MongoDBCacheManagementService underTest() {
		return new MongoDBCacheManagementService()
	}

}
*/