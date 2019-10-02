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
import com.zions.common.services.test.SpockLabeler
import com.zions.common.services.work.handler.IFieldHandler
import com.zions.qm.services.test.handlers.NameHandler
import com.zions.qm.services.test.handlers.QmBaseAttributeHandler
import com.zions.qm.services.test.handlers.StartDateHandler
import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[ClmTestItemManagementServiceSpecConfig])
class ClmTestItemManagementServiceSpec extends Specification implements SpockLabeler {

	@Autowired
	ClmTestItemManagementService underTest

	@Autowired
	TestMappingManagementService testMappingManagementService

	@Autowired
	DataGenerationService dataGenerationService
	
	@Autowired
	ICacheManagementService cacheManagementService
	
//	@Autowired
//	Map<String, IFieldHandler> fieldMap

	def 'getChanges main flow with test plan data'() {
		given: g_ 'setup plan data'
		//Plan data
		def testplan = dataGenerationService.generate('/testdata/testplan218.xml')

		and: a_ 'setup team map'
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

		when: w_ 'call getChanges'
		underTest.processForChanges('DigitalBanking', testplan, teamMap) { 
			key, item -> }

		then: t_ null
		true
	}

	def 'getChanges main flow with test suite data'() {
		given: g_ 'setup suite data'
		//Plan data
		def testsuite = dataGenerationService.generate('/testdata/testsuiteT.xml')
		def testplan = dataGenerationService.generate('/testdata/TestPlan.json')

		and: a_ 'team map'
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
		
		when: w_ 'call method under test (getChanges).'
		underTest.processForChanges('DigitalBanking', testsuite, teamMap, null, null, testplan) { 
			key, item -> }
		then: t_ 'ensure change data'
		true
	}

	def 'getChanges main flow with test case data'() {
		given: g_ 'setup test case data'
		//Plan data
		def testcase = dataGenerationService.generate('/testdata/testcaseT.xml')

		and: a_ 'setup team map'
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

		when: w_ 'call getChanges'
		underTest.processForChanges('DigitalBanking', testcase, teamMap) { 
			key, item -> }

		then: t_ null
		true
	}
	def 'getChanges main flow with configuration data'() {
		given: g_ 'setup of configuration data'
		//Plan data
		def configuration = dataGenerationService.generate('/testdata/configurationT.xml')

		and: a_ 'setup team map'
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

		when: w_ 'call method under test (getChanges).'
		underTest.processForChanges('DigitalBanking', configuration, teamMap) { 
			key, item -> }

		then: t_ null
		true
	}

	def 'getChanges main flow with execution result data'() {
		given: g_ 'Result data'
		//Plan data
		def executionresults = dataGenerationService.generate('/testdata/executionresults1.xml')
		def erlist = executionresults.'**'.findAll { it.name() == 'executionresult' }
		def outItems = []
		erlist.each { item ->
			outItems.add(item)
		}
		and: a_ 'Test case data'
		def testcase = dataGenerationService.generate('/testdata/testcaseT.xml')

		and: a_ 'result map data'
		def result = dataGenerationService.generate('/testdata/resultsMap.json')
		def tcMap = [:]
		result.'value'.each { aresult ->
			tcMap["${aresult.testCase.id}"] = aresult
		}
		
		and: a_ 'stub call to get test case from cache'
		1 * cacheManagementService.getFromCache(_, _) >> dataGenerationService.generate('/testdata/testcase1.json')

		and: a_ 'setup team map data'
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

		when: w_ 'call getChanges'
		underTest.processForChanges('DigitalBanking', outItems[0], teamMap, tcMap, testcase) { 
			key, item -> }

		then: t_ null
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
	Map<String, QmBaseAttributeHandler> fieldMap() {
		Map<String, QmBaseAttributeHandler> retVal = ['QmNameHandler': new NameHandler(),
			'QmStartDateHandler': new StartDateHandler()
		]
		return retVal
	}

	@Bean
	DataGenerationService dataGenerationService() {
		return new DataGenerationService()
	}

}