package com.zions.ext.services.cache;

import static org.junit.Assert.*

import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.qm.services.metadata.QmMetadataManagementService
import com.zions.qm.services.project.QmProjectManagementService
import com.zions.qm.services.test.ClmTestManagementService
import groovy.json.JsonSlurper
import com.zions.bb.services.code.BBCodeManagementService
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
	
	def 'saveBinaryAsAttachment for project name success flow.'(){

	def result= new ByteArrayInputStream();
		
	when: 'calling of method under test (getKey)'
	def keyname = underTest.saveBinaryAsAttachment( result ,'','')
	// 218-Test Plan
	then: ''
	true
	
}

}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
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
