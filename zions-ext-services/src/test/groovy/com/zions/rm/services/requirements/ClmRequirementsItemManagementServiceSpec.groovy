package com.zions.rm.services.requirements

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[ClmRequirementsItemManagementServiceSpecConfig])
class ClmRequirementsItemManagementServiceSpec extends Specification {

	def 'Main flow for getChanges'() {
		setup: 'Stubs for dependencies'
		
		when: 'Run getChanges'
		
		then: 'Validate resulting changes'
		
	}

}

@TestConfiguration
@Profile("test")
//@ComponentScan(["com.zions.common.services.test"])
@PropertySource("classpath:test.properties")
class ClmRequirementsItemManagementServiceSpecConfig {
	def factory = new DetachedMockFactory()
}
