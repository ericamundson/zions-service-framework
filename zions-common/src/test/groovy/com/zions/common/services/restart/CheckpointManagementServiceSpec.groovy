package com.zions.common.services.restart

import static org.junit.Assert.*

import com.mongodb.Mongo
import com.mongodb.MongoClient
import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.test.SpockLabeler

import org.junit.Test
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

@ContextConfiguration(classes=[CheckpointManagementServiceSpecConfig])
class CheckpointManagementServiceSpec extends Specification implements SpockLabeler {
	@Autowired
	ICheckpointManagementService underTest
	
	@Autowired
	ICacheManagementService cacheManagementService

	def 'selectCheckpoint last key'() {
		setup: s_ 'some test checkpoints'
		cacheManagementService.clear();
		underTest.resetIdCounter()
		underTest.addCheckpoint('test', 'page1')
		underTest.addCheckpoint('test', 'page2')
		underTest.addCheckpoint('test', 'page3')
		underTest.addCheckpoint('test', 'page4')
		
		when: w_ 'call selectCheckpoint'
		Checkpoint cp = underTest.selectCheckpoint('last')
		
		then: t_ 'cp.pageUrl == page4'
		cp.pageUrl == 'page4'
	}
	
	def 'selectCheckpoint priorToLogEntries key'() {
		setup: s_ 'checkpoint items'
		cacheManagementService.clear();
		underTest.resetIdCounter()
		underTest.addCheckpoint('test', 'page1')
		underTest.addCheckpoint('test', 'page2')
		underTest.addCheckpoint('test', 'page3')
		underTest.addLogentry('stuff1')
		underTest.addLogentry('stuff2')
		underTest.addCheckpoint('test', 'page4')
		
		when: w_ 'call selectCheckpoint with log entries'
		Checkpoint cp = underTest.selectCheckpoint('priorToLogEntries')
		
		then: t_ 'No exceptions'
		cp.pageUrl == 'page2'
	}
	
	def 'selectCheckpoint specific key'() {
		setup: s_ 'add checkpoints'
		cacheManagementService.clear();
		underTest.resetIdCounter()
		underTest.addCheckpoint('test', 'page1')
		underTest.addCheckpoint('test', 'page2')
		underTest.addCheckpoint('test', 'page3')
		underTest.addLogentry('stuff1')
		underTest.addLogentry('stuff2')
		underTest.addCheckpoint('test', 'page4')
		
		when: w_ 'call selectCheckpoint with specific key'
		Checkpoint cp = underTest.selectCheckpoint('2-checkpoint')
		
		then: t_ "cp.pageUrl == 'page3'"
		cp.pageUrl == 'page3'
	}

}


@TestConfiguration
@Profile("test")
//@ComponentScan(["com.zions.common.services.cache.db"])
//@EnableMongoRepositories(basePackages = "com.zions.common.services.cache.db")
@PropertySource("classpath:test.properties")
class CheckpointManagementServiceSpecConfig {
	@Autowired
	@Value('${cache.location:}')
	String cacheLocation
	
	@Bean
	ICheckpointManagementService underTest()
	{
		return new CheckpointManagementService()
	}
	
	@Bean
	ICacheManagementService cacheManagementService() {
//		return new MongoDBCacheManagementService()
		return new CacheManagementService()
	}

//	@Bean
//	MongoClient mongoClient() throws UnknownHostException {
//		return new MongoClient("localhost");
//	}
//	
//	public @Bean MongoTemplate mongoTemplate() throws Exception {
//		return new MongoTemplate(mongoClient(), "adomigration");
//	}
  
}