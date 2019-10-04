package com.zions.clm.services.rtc.project.members;

import static org.junit.Assert.*

import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.SpockLabeler
import groovy.json.JsonSlurper

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[CcmMemberManagementServiceTestConfig])
public class CcmMemberManagementServiceTest extends Specification {
	
	@Autowired
	IGenericRestClient clmGenericRestClient
	
	@Autowired
	CcmMemberManagementService underTest
	
	def 'getMemberData success flow.'() {
		given: "A stub of RQM get test item request"
		def testplansInfo = new XmlSlurper().parseText(this.getClass().getResource('/testdata/ccmworkitemtype.xml').text)
		1 * clmGenericRestClient.get(_) >> testplansInfo

		when: 'calling of method under test (getMemberData)'
		def testPlans = underTest.getMemberData('project','tfsproject')
		
		then: 'No exception'
		true
	}
	
	def 'buildMemberData success flow.'() {
		/*given: "A stub of RQM get test item request"
		def testplansInfo = new XmlSlurper().parseText(this.getClass().getResource('/testdata/workitemtype.xml').text)
		1 * clmGenericRestClient.get(_) >> testplansInfo*/
		def teamInfo = new XmlSlurper().parseText(getClass().getResource('/testdata/testmember.xml').text)
		when: 'calling of method under test (buildMemberData)'
		def testPlans = underTest.buildMemberData( teamInfo,'tfsproject')
		
		then: 'No exception'
		true
	}
	
	def 'getNextPage success flow.'() {
		given: "A stub of RQM get nextpage test item request"
		def testplansInfo = new XmlSlurper().parseText(this.getClass().getResource('/testdata/nextpage.xml').text)
		1 * clmGenericRestClient.get(_) >> testplansInfo

		when: 'calling of method under test (getNextPage)'
		def testPlans = underTest.nextPage('https://clm.cs.zionsbank.com/qm/service/com.ibm.rqm.integration.service.IIntegrationService/resources/Zions+FutureCore+Program+%28Quality+Management%29/testplan?token=_TJVcwOKdEeirC8bfvJTPjw&amp;page=1')
		
		then: 'valid list of plans'
		testPlans.entry.size() > 0
	}

}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class CcmMemberManagementServiceTestConfig {
	def factory = new DetachedMockFactory()
	
	@Bean
	IGenericRestClient clmGenericRestClient() {
		return factory.Mock(ClmGenericRestClient)
	}
	
	@Bean
	CcmMemberManagementService underTest() {
		return new CcmMemberManagementService()
	}
}
