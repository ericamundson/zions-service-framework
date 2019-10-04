package com.zions.qm.services.test

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.SpockLabeler
import com.zions.clm.services.rest.ClmGenericRestClient
import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[TestMappingManagementServiceSpecTestConfig])
class TestMappingManagementServiceTest extends Specification {
	
	@Autowired
	TestMappingManagementService underTest

	def 'getMappingData success flow.'() {
		
		when: 'calling method under test (getMappingData)'
		def mapping = underTest.getMappingData()
		
		then:  'mapping.wit.size() > 0'
		mapping.wit.size() > 0
	}

}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class TestMappingManagementServiceSpecTestConfig {
	def factory = new DetachedMockFactory()
	
	
	@Bean
	TestMappingManagementService underTest() {
		return new TestMappingManagementService()
	}
}

