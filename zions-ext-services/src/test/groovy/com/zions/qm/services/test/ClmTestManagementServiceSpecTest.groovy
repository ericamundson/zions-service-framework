package com.zions.qm.services.test

import static org.junit.Assert.*

import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.DataGenerationService

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
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
	
	@Autowired
	DataGenerationService dataGenerationService

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
	
	def 'getConfigurationsViaQuery success flow.'() {
		given: "A stub of RQM get test item request"
		def configurationsInfo = dataGenerationService.generate('/testdata/configurations.xml')
		1 * qmGenericRestClient.get(_) >> configurationsInfo

		when: 'calling of method under test (getConfigurationsViaQuery)'
		def configurations = underTest.getConfigurationsViaQuery('', 'DigitalBanking')
		
		then: 'validate list of configurations'
		configurations.entry.size() > 0
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
	
	def 'getExecutionResultViaHref no execution.'() {
		given: "A stub of RQM get execution results request"
		def testplansInfo = new XmlSlurper().parseText(this.getClass().getResource('/testdata/executionresults.xml').text)
		1 * qmGenericRestClient.get(_) >> testplansInfo

		when: 'calling of method under test (getNextPage)'
		def executionresults = underTest.getExecutionResultViaHref('123', '456', 'aproject')
		
		then: 'validate list of results'
		executionresults.size() == 0
	}
	
	def 'getExecutionResultViaHref success flow.'() {
		given: "A stub of RQM get execution results request"
		def testplansInfo = new XmlSlurper().parseText(this.getClass().getResource('/testdata/executionresults1.xml').text)
		1 * qmGenericRestClient.get(_) >> testplansInfo

		when: 'calling of method under test (getNextPage)'
		def executionresults = underTest.getExecutionResultViaHref('123', '578', 'aproject')
		
		then: 'validate list of results'
		executionresults.size() == 1
	}
	
	def 'getContent success flow.'() {
		given: "A stub of RQM request to get attachment with headers"
		File file = new File('563414- Product Classifications Table')
		def of = file.newDataOutputStream()
		of.close()
		1 * qmGenericRestClient.get(_) >> [data: of, headers: ['Content-Disposition': 'filename="563414- Product Classifications Table"']]
		
		when: 'Call method under test (getContent)'
		def result = underTest.getContent('http://someimage')
		
		then: 'Validate result data'
		result.headers.containsKey('Content-Disposition')
	}
}

@TestConfiguration
@Profile("test")
@ComponentScan(["com.zions.common.services.test"])
@PropertySource("classpath:test.properties")
class ClmTestManagementServiceSpecTestConfig {
	def factory = new DetachedMockFactory()
	
	@Bean
	IGenericRestClient qmGenericRestClient() {
		return factory.Mock(ClmGenericRestClient)
	}
	
	@Bean
	ClmTestManagementService underTest() {
		return new ClmTestManagementService()
	}
	
	@Bean
	DataGenerationService dataGenerationService() {
		return new DataGenerationService()
	}

}


