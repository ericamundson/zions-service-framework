package com.zions.clm.services.rtc.project.workitems;

import static org.junit.Assert.*

import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.common.services.rest.IGenericRestClient
import com.zions.qm.services.test.ClmTestManagementService

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[ClmWorkItemManagementServiceTestConfig])

public class ClmWorkItemManagementServiceTest extends Specification {
	
	@Autowired
	IGenericRestClient clmGenericRestClient
	
	@Autowired
	ClmWorkItemManagementService underTest
	
	def 'getWorkItemHistory success flow.'() {
		given: "A stub of RQM get test item request"
		def testplansInfo = new XmlSlurper().parseText(this.getClass().getResource('/testdata/testplansquery.xml').text)
		1 * clmGenericRestClient.get(_) >> testplansInfo

		when: 'calling of method under test (getTestPlansViaQuery)'
		def testPlans = underTest.getWorkItemHistory(22657)
		
		then: 'validate list of plans'
		testPlans.entry.size() > 0
	}
	
	def 'getWorkItemsViaQuery success flow.'() {
		given: "A stub of RQM get test item request"
		def testplansInfo = new XmlSlurper().parseText(this.getClass().getResource('/testdata/testplansquery.xml').text)
		1 * clmGenericRestClient.get(_) >> testplansInfo

		when: 'calling of method under test (getTestPlansViaQuery)'
		def testPlans = underTest.getWorkItemsViaQuery('')
		
		then: 'validate list of plans'
		testPlans.entry.size() > 0
	}
	
	def 'getNextPage success flow.'() {
		given: "A stub of RQM get test item request"
		def testplansInfo = new XmlSlurper().parseText(this.getClass().getResource('/testdata/nextpage.xml').text)
		1 * clmGenericRestClient.get(_) >> testplansInfo

		when: 'calling of method under test (getNextPage)'
		def testPlans = underTest.nextPage('https://clm.cs.zionsbank.com/qm/service/com.ibm.rqm.integration.service.IIntegrationService/resources/Zions+FutureCore+Program+%28Quality+Management%29/testplan?token=_TJVcwOKdEeirC8bfvJTPjw&amp;page=1')
		
		then: 'validate list of plans'
		testPlans.entry.size() > 0
	}

}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class ClmWorkItemManagementServiceTestConfig {
	def factory = new DetachedMockFactory()
	
	@Bean
	IGenericRestClient clmGenericRestClient() {
		return factory.Mock(ClmGenericRestClient)
	}
	
	@Bean
	ClmWorkItemManagementService underTest() {
		return new ClmWorkItemManagementService()
	}
}
