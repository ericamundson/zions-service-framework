package com.zions.common.services.cache;


import groovy.json.JsonSlurper

import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.DataGenerationService
import com.zions.common.services.test.SpockLabeler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[CacheManagementServiceTestConfig])
public class CacheManagementServiceTest extends Specification {

	@Autowired
	CacheManagementService underTest
	
	@Autowired
	DataGenerationService dataGenerationService
	

	def 'saveBinaryAsAttachment for project name success flow.'(){

		def result= new ByteArrayInputStream();

		when: 'calling of method under test (saveBinaryAsAttachment)'
		def keyname = underTest.saveBinaryAsAttachment( result ,'','')
		// 218-Test Plan
		then: 'No exception'
		true

	}

	def 'saveToCache for project name success flow.'(){

		def data = dataGenerationService.generate('/testdata/TestPlanT_Cache.json')

		when: 'calling of method under test (data)'
		def keyname = underTest.saveToCache( data ,'','')

		then: 'No save failure'
		true
	}

	def 'getFromCache for project name success flow.'(){

		def data = dataGenerationService.generate('/testdata/TestPlanT_Cache.json')

		when: 'calling of method under test (getFromCache)'
		def keyname = underTest.getFromCache( '','')

		then: 'No save failure'
		true
	}
}

@TestConfiguration
@Profile("test")
@ComponentScan(["com.zions.common.services.test"])
@PropertySource("classpath:test.properties")
class CacheManagementServiceTestConfig {
	def factory = new DetachedMockFactory()

	@Autowired
	@Value('${cache.location:}')
	String cacheLocation


	@Bean
	CacheManagementService underTest() {
		return new CacheManagementService(cacheLocation)
	}
	
	@Bean
	DataGenerationService dataGenerationService() {
		return new DataGenerationService()
	}
}
