package com.zions.vsts.services.test

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource

import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.tfs.rest.GenericRestClient
import groovy.json.JsonBuilder
import spock.lang.Specification
import spock.mock.DetachedMockFactory

class TestManagementServiceSpec extends Specification {
	
	@Autowired
	IGenericRestClient realGenericRestClient

	
	def 'getTestWorkItems success flow.'() {
		//fail("Not yet implemented")
	}
	
	def 'setup test associations'() {
//		given:
//		def assoc = [sequenceNumber: 0, id: 21546, suiteEntryType: 'suite']
//		String body = new JsonBuilder(assoc).toPrettyString()
//		
//		when:
//		String url = "${realGenericRestClient.getTfsUrl()}/DigitalBanking//_apis/testplan/suiteentry/"
//		def result = realGenericRestClient.patch
		
	}

}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class TestManagementServiceSpecConfig {
	def mockFactory = new DetachedMockFactory()
	
	@Bean
	IGenericRestClient realGenericRestClient() {
		return new GenericRestClient('https://dev.azure.com/eto-dev', 'z091182', 'nne526gq4vgseefkdn25v4cw2pm74qsacn2ylkhlqjltrd4oalvq')
	}

}