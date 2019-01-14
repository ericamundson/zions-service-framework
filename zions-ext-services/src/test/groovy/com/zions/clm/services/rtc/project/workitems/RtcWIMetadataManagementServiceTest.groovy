package com.zions.clm.services.rtc.project.workitems;

import static org.junit.Assert.*

import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.common.services.rest.IGenericRestClient
import com.zions.clm.services.rtc.project.workitems.RtcWIMetadataManagementService

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import spock.lang.Specification
import spock.mock.DetachedMockFactory

@Deprecated
@ContextConfiguration(classes=[RtcWIMetadataManagementServiceTestConfig])
public class RtcWIMetadataManagementServiceTest{
	
	@Autowired
	IGenericRestClient clmGenericRestClient
	
	@Autowired
	RtcWIMetadataManagementService underTest
	
	def 'getWorkItemTypes success flow.'() {
		given: "A stub of RQM get test item request"
		def testplansInfo = new XmlSlurper().parseText(this.getClass().getResource('/testdata/workitemtype.xml').text)
		1 * clmGenericRestClient.get(_) >> testplansInfo

		when: 'calling of method under test (getWorkItemTypes)'
		def testPlans = underTest.getWorkItemTypes('projectArea')
		
		then: 'validate list of plans'
		testPlans.entry.size() > 0
	}
	
	def 'getCustomAttributes success flow.'() {
		given: "A stub of RQM get test item request"
		def testplansInfo = new XmlSlurper().parseText(this.getClass().getResource('/testdata/workitemtype.xml').text)
		1 * clmGenericRestClient.get(_) >> testplansInfo

		when: 'calling of method under test (getCustomAttributes)'
		def testPlans = underTest.getCustomAttributes('projectArea', 'wit')
		
		then: 'validate list of plans'
		testPlans.entry.size() > 0
	}
	
	def 'extractTypeMetadata success flow.'() {
			
		File file = new File('563414- Product Classifications Table')
		def of = file.newDataOutputStream()
		of.close()

		when: 'calling of method under test (extractTypeMetadata)'
		def testPlans = underTest.extractTypeMetadata('projectArea', 'wit', '563414- Product Classifications Table')
		
		then: ''
		true
	}
	
	def 'extractWorkitemMetadata success flow.'() {
		
		
	given: "A stub of RQM get test item request"
	def testplansInfo = new XmlSlurper().parseText(this.getClass().getResource('/testdata/rtcworkitemtype.xml').text)
	1 * clmGenericRestClient.get(_) >> testplansInfo
	
	File file = new File('Samplework item data')
	def of = file.newDataOutputStream()
	of.close()

	when: 'calling of method under test (extractWorkitemMetadata)'
	def testPlans = underTest.extractWorkitemMetadata('projectArea','Samplework item data')
	
	then: ''
	true
}

}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class RtcWIMetadataManagementServiceTestConfig {
	def factory = new DetachedMockFactory()
	
	@Bean
	IGenericRestClient clmGenericRestClient() {
		return factory.Mock(ClmGenericRestClient)
	}
	
	@Bean
	RtcWIMetadataManagementService underTest() {
		return new RtcWIMetadataManagementService()
	}
}