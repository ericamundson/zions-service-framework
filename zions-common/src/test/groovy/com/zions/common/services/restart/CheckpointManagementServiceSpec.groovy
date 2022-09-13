package com.zions.common.services.restart

import static org.junit.Assert.*

import com.mongodb.Mongo
import com.mongodb.client.MongoClient
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
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.test.context.ContextConfiguration

import spock.lang.Specification

@ContextConfiguration(classes=[CheckpointManagementServiceSpecConfig])
class CheckpointManagementServiceSpec extends Specification {
	@Autowired
	ICheckpointManagementService underTest
	
	@Autowired
	ICacheManagementService cacheManagementService

	def 'selectCheckpoint last key'() {
		setup: 'some test checkpoints'
		cacheManagementService.clear();
		underTest.resetIdCounter()
		underTest.addCheckpoint('test', 'page1')
		underTest.addCheckpoint('test', 'page2')
		underTest.addCheckpoint('test', 'page3')
		underTest.addCheckpoint('test', 'page4')
		
		when: 'call selectCheckpoint'
		Checkpoint cp = underTest.selectCheckpoint('last')
		
		then: 'cp.pageUrl == page4'
		cp.pageUrl == 'page4'
	}
	
	def 'selectCheckpoint priorToLogEntries key'() {
		setup: 'checkpoint items'
		cacheManagementService.clear();
		underTest.resetIdCounter()
		underTest.addCheckpoint('test', 'page1')
		underTest.addCheckpoint('test', 'page2')
		underTest.addCheckpoint('test', 'page3')
		underTest.addLogentry('stuff1')
		underTest.addLogentry('stuff2')
		underTest.addCheckpoint('test', 'page4')
		
		when: 'call selectCheckpoint with log entries'
		Checkpoint cp = underTest.selectCheckpoint('priorToLogEntries')
		
		then: 'No exceptions'
		cp.pageUrl == 'page2'
	}
	
	def 'selectCheckpoint specific key'() {
		setup: 'add checkpoints'
		cacheManagementService.clear();
		underTest.resetIdCounter()
		underTest.addCheckpoint('test', 'page1')
		underTest.addCheckpoint('test', 'page2')
		underTest.addCheckpoint('test', 'page3')
		underTest.addLogentry('stuff1')
		underTest.addLogentry('stuff2')
		underTest.addCheckpoint('test', 'page4')
		
		when: 'call selectCheckpoint with specific key'
		Checkpoint cp = underTest.selectCheckpoint('2-checkpoint')
		
		then: "cp.pageUrl == 'page3'"
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

  
}