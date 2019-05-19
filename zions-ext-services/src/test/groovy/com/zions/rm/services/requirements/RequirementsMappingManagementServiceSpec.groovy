package com.zions.rm.services.requirements

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import spock.lang.Specification

@ContextConfiguration(classes=[RequirementsMappingManagementServiceSpecConfig])
class RequirementsMappingManagementServiceSpec extends Specification {

	@Autowired
	RequirementsMappingManagementService underTest
	
	def 'Main flow for getMappingData'() {
		setup: 'Nothing required'
		
		when: 'Call getMappingData'
		def map = underTest.getMappingData()
		
		then: 'Validate map'
		map.size() == 3
	}

}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class RequirementsMappingManagementServiceSpecConfig {
	
	@Bean
	RequirementsMappingManagementService underTest() {
		return new RequirementsMappingManagementService()
	}
	
}
