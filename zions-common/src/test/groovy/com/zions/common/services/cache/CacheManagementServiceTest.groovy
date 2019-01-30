package com.zions.common.services.cache;


import static org.junit.Assert.*
import groovy.json.JsonSlurper

import com.zions.common.services.rest.IGenericRestClient

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[CacheManagementServiceTestConfig])
public class CacheManagementServiceTest extends Specification {
	
	@Autowired
	CacheManagementService underTest
	
	@Test
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
		
	def data = new JsonSlurper().parseText(getClass().getResource('/testdata/TestPlanT_Cache.json').text)
				
	when: 'calling of method under test (data)'
	def keyname = underTest.saveToCache( data ,'','')

	then: ''
	true
		
	}
	
	@Test
	def 'getFromCache for project name success flow.'(){
		
	def data = new JsonSlurper().parseText(getClass().getResource('/testdata/TestPlanT_Cache.json').text)
				
	when: 'calling of method under test (getFromCache)'
	def keyname = underTest.getFromCache( '','')

	then: ''
	true
		
	}

}

@TestConfiguration
@Profile("test")
class CacheManagementServiceTestConfig {
	def factory = new DetachedMockFactory()
	
	@Autowired
	@Value('${cache.location}')
	String cacheLocation
	
	
	@Bean
	CacheManagementService underTest() {
		return new CacheManagementService()
	}
	
}
