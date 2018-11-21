package com.zions.qm.services.test

import static org.junit.Assert.*

import com.zions.common.services.rest.IGenericRestClient
import com.zions.qm.services.rest.QmGenericRestClient
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

@ContextConfiguration(classes=[ClmTestManagementServiceSpecTestConfig])
class ClmTestManagementServiceSpecTest extends Specification {
	
	
	@Autowired
	IGenericRestClient qmGenericRestClient
	
	@Autowired
	ClmTestManagementService underTest

	def 'getTestItem success flow.'() {
		given: "A stub of RQM get test item request"
		def testcaseInfo = new XmlSlurper().parseText(this.getClass().getResource('/testdata/testcase.xml').text)
		1 * qmGenericRestClient.get(_) >> testcaseInfo
		
		when: "calling of method under test (getTestItem)"
		String uri = 'https://clm.cs.zionsbank.com/qm/service/com.ibm.rqm.integration.service.IIntegrationService/resources/_aC9CQPfREeOd1div3hxkJQ/testcase/BS-82'
		def testcaseData = underTest.getTestItem(uri)
		
		then: "validate test item"
		"${testcaseData.webId.text()}" == '22657'
	}
	
	def 'getTestPlansViaQuery success flow.'() {
		given: "A stub of RQM get test item request"
		def testplansInfo = new XmlSlurper().parseText(this.getClass().getResource('/testdata/testplansquery.xml').text)
		1 * qmGenericRestClient.get(_) >> testplansInfo

		when: 'calling of method under test (getTestPlansViaQuery)'
		def testPlans = underTest.getTestPlansViaQuery('', 'DigitalBanking')
		
		then: 'validate list of plans'
		testPlans.entry.size() > 0
	}

	def 'getNextPage success flow.'() {
		given: "A stub of RQM get test item request"
		def testplansInfo = new XmlSlurper().parseText(this.getClass().getResource('/testdata/nextpage.xml').text)
		1 * qmGenericRestClient.get(_) >> testplansInfo

		when: 'calling of method under test (getNextPage)'
		def testPlans = underTest.nextPage('https://clm.cs.zionsbank.com/qm/service/com.ibm.rqm.integration.service.IIntegrationService/resources/Zions+FutureCore+Program+%28Quality+Management%29/testplan?token=_TJVcwOKdEeirC8bfvJTPjw&amp;page=1')
		
		then: 'validate list of plans'
		testPlans.entry.size() > 0
	}
}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class ClmTestManagementServiceSpecTestConfig {
	def factory = new DetachedMockFactory()
	
	@Bean
	IGenericRestClient qmGenericRestClient() {
		return factory.Mock(QmGenericRestClient)
	}
	
	@Bean
	ClmTestManagementService underTest() {
		return new ClmTestManagementService()
	}
}


