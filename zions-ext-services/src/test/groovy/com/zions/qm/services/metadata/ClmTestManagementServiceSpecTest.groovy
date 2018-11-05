package com.zions.qm.services.metadata

import static org.junit.Assert.*

import com.zions.common.services.rest.IGenericRestClient
import com.zions.qm.services.rest.QmGenericRestClient
import com.zions.qm.services.test.ClmTestManagementService
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
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

}

@TestConfiguration
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
