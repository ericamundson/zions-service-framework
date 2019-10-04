package com.zions.bb.services.code;

import static org.junit.Assert.*

import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.qm.services.test.ClmTestManagementService
import groovy.json.JsonSlurper
import com.zions.bb.services.code.BBCodeManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.SpockLabeler
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[BBCodeManagementServiceTestConfig])
public class BBCodeManagementServiceTest extends Specification {
	
	@Autowired
	IGenericRestClient bBGenericRestClient
	
	@Autowired
	BBCodeManagementService underTest
	
	def 'getProjectRepoUrls for project name success flow.'(){
		
		setup: 'stub call for to get all projects'		
		def testplan = new JsonSlurper().parseText(getClass().getResource('/testdata/allprojects.json').text)
		(1..3) * bBGenericRestClient.get(_) >> testplan
		
		and: 'stub rest call for repos'
		def test = new JsonSlurper().parseText(getClass().getResource('/testdata/allprojects_lastpage_false.json').text)
		1 * bBGenericRestClient.get(_) >> test
		
		when: 'calling of method under test (getProjectRepoUrls)'
		def keyname = underTest.getProjectRepoUrls('almops')
		
		then: 'No exception'
		true
		
	}

}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class BBCodeManagementServiceTestConfig {
	def factory = new DetachedMockFactory()
	
	@Bean
	IGenericRestClient bBGenericRestClient() {
		return factory.Mock(ClmGenericRestClient)
	}
	
	@Bean
	BBCodeManagementService underTest() {
		return new BBCodeManagementService()
	}
}
