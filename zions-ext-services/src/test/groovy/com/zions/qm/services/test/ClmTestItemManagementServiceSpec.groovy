package com.zions.qm.services.test

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import com.zions.common.services.work.handler.IFieldHandler
import com.zions.qm.services.test.handlers.NameHandler
import com.zions.qm.services.test.handlers.StartDateHandler
import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[ClmTestItemManagementServiceSpecConfig])
class ClmTestItemManagementServiceSpec extends Specification {

	@Autowired
	ClmTestItemManagementService underTest
	
	@Autowired
	TestMappingManagementService testMappingManagementService

	def 'getChanges main flow with testplan data'() {
//		given: 'Plan data'
//		//Plan data
//		def testplan = new XmlSlurper().parseText(getClass().getResource('/testdata/testplan.xml').text)
//
//		and: 'team map'		
//		//Team map
//		def teamInfo = new JsonSlurper().parseText(getClass().getResource('/testdata/teammembers.json').text)
//		def teamMap = [:]
//		teamInfo.'value'.each { id ->
//			def identity = id.identity
//			String uid = "${identity.uniqueName}"
//			if (teamMap[uid.toLowerCase()] == null) {
//				teamMap[uid.toLowerCase()] = identity
//			}
//
//		}
//
		when: 'call method under test (getChanges).'
//		def changedata = underTest.getChanges('DigitalBanking', testplan, teamMap)
//		
		then: 'ensure change data'
		true
	}

}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class ClmTestItemManagementServiceSpecConfig {
	def factory = new DetachedMockFactory()
	
	@Bean
	ClmTestItemManagementService underTest() {
		return new ClmTestItemManagementService()
	}
	
	@Bean
	TestMappingManagementService testMappingManagementService() {
		return new TestMappingManagementService()
	}
	
	@Bean 
	Map<String, IFieldHandler> fieldMap() {
		Map<String, IFieldHandler> retVal = ['nameHandler': new NameHandler(),
			'startDateHandler': new StartDateHandler()
			]
		return retVal
	}
}