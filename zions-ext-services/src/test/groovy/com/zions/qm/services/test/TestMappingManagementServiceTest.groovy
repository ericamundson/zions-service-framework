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
import com.zions.qm.services.rest.QmGenericRestClient
import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[TestMappingManagementServiceSpecTestConfig])
class TestMappingManagementServiceTest extends Specification {
	
	@Autowired
	TestMappingManagementService underTest

	def 'getMappingData success flow.'() {
		given: 'No stub.'
		
		when: 'calling method under test (getMappingData)'
		def mapping = underTest.getMappingData()
		
		then:  'validate mapping data.'
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

