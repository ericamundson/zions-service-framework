package com.zions.qm.services.test

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.test.DataGenerationService
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

	@Autowired
	DataGenerationService dataGenerationService
	
	@Autowired
	ICacheManagementService cacheManagementService

	def 'getChanges main flow with test plan data'() {
		given: 'Plan data'
		//Plan data
		def testplan = dataGenerationService.generate('/testdata/testplan218.xml')

		and: 'team map'
		//Team map
		def teamInfo = dataGenerationService.generate('/testdata/teammembers.json')
		def teamMap = [:]
		teamInfo.'value'.each { id ->
			def identity = id.identity
			String uid = "${identity.uniqueName}"
			if (teamMap[uid.toLowerCase()] == null) {
				teamMap[uid.toLowerCase()] = identity
			}

		}

		when: 'call method under test (getChanges).'
		def changedata = underTest.getChanges('DigitalBanking', testplan, teamMap)

		then: 'ensure change data'
		true
	}

	def 'getChanges main flow with test suite data'() {
		given: 'Suite data'
		//Plan data
		def testsuite = dataGenerationService.generate('/testdata/testsuiteT.xml')
		def testplan = dataGenerationService.generate('/testdata/TestPlan.json')

		and: 'team map'
		//Team map
		def teamInfo = dataGenerationService.generate('/testdata/teammembers.json')
		def teamMap = [:]
		teamInfo.'value'.each { id ->
			def identity = id.identity
			String uid = "${identity.uniqueName}"
			if (teamMap[uid.toLowerCase()] == null) {
				teamMap[uid.toLowerCase()] = identity
			}

		}
		
		when: 'call method under test (getChanges).'
		def changedata = underTest.getChanges('DigitalBanking', testsuite, teamMap, null, null, testplan)

		then: 'ensure change data'
		true
	}

	def 'getChanges main flow with test case data'() {
		given: 'Plan data'
		//Plan data
		def testcase = dataGenerationService.generate('/testdata/testcase49884.xml')

		and: 'team map'
		//Team map
		def teamInfo = dataGenerationService.generate('/testdata/teammembers.json')
		def teamMap = [:]
		teamInfo.'value'.each { id ->
			def identity = id.identity
			String uid = "${identity.uniqueName}"
			if (teamMap[uid.toLowerCase()] == null) {
				teamMap[uid.toLowerCase()] = identity
			}

		}

		when: 'call method under test (getChanges).'
		def changedata = underTest.getChanges('DigitalBanking', testcase, teamMap)

		then: 'ensure change data'
		true
	}
	def 'getChanges main flow with configuration data'() {
		given: 'Plan data'
		//Plan data
		def configuration = dataGenerationService.generate('/testdata/configurationT.xml')

		and: 'team map'
		//Team map
		def teamInfo = dataGenerationService.generate('/testdata/teammembers.json')
		def teamMap = [:]
		teamInfo.'value'.each { id ->
			def identity = id.identity
			String uid = "${identity.uniqueName}"
			if (teamMap[uid.toLowerCase()] == null) {
				teamMap[uid.toLowerCase()] = identity
			}

		}

		when: 'call method under test (getChanges).'
		def changedata = underTest.getChanges('DigitalBanking', configuration, teamMap)

		then: 'ensure change data'
		true
	}

	def 'getChanges main flow with execution result data'() {
		given: 'Result data'
		//Plan data
		def executionresults = dataGenerationService.generate('/testdata/executionresults1.xml')
		def erlist = executionresults.'**'.findAll { it.name() == 'executionresult' }
		def outItems = []
		erlist.each { item ->
			outItems.add(item)
		}
		and: 'Test case data'
		def testcase = dataGenerationService.generate('/testdata/testcase47598.xml')

		and: 'result map data'
		def result = dataGenerationService.generate('/testdata/resultsMap.json')
		def tcMap = [:]
		result.'value'.each { aresult ->
			tcMap["${aresult.testCase.id}"] = aresult
		}
		
		and: 'stub call to get test case from cache'
		1 * cacheManagementService.getFromCache(_, _) >> dataGenerationService.generate('/testdata/testcase1.json')

		and: 'team map'
		//Team map
		def teamInfo = dataGenerationService.generate('/testdata/teammembers.json')
		def teamMap = [:]
		teamInfo.'value'.each { id ->
			def identity = id.identity
			String uid = "${identity.uniqueName}"
			if (teamMap[uid.toLowerCase()] == null) {
				teamMap[uid.toLowerCase()] = identity
			}

		}

		when: 'call method under test (getChanges).'
		def changedata = underTest.getChanges('DigitalBanking', outItems[0], teamMap, tcMap, testcase)

		then: 'ensure change data'
		true
	}

}

@TestConfiguration
@Profile("test")
@ComponentScan(["com.zions.common.services.test"])
@PropertySource("classpath:test.properties")
class ClmTestItemManagementServiceSpecConfig {
	def factory = new DetachedMockFactory()

	@Bean
	ClmTestItemManagementService underTest() {
		return new ClmTestItemManagementService()
	}

	@Autowired
	@Value('${cache.location}')
	String cacheLocation
	@Bean
	ICacheManagementService cacheManagementService() {
		return factory.Mock(CacheManagementService)
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

	@Bean
	DataGenerationService dataGenerationService() {
		return new DataGenerationService()
	}

}