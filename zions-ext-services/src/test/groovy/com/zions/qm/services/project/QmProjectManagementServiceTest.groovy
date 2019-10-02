package com.zions.qm.services.project;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.qm.services.project.QmProjectManagementService;
import com.zions.common.services.rest.IGenericRestClient;
import com.zions.common.services.test.SpockLabeler

import static org.junit.Assert.*

import groovy.json.JsonSlurper
import com.zions.bb.services.code.BBCodeManagementService
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[QmProjectManagementServiceTestConfig])
public class QmProjectManagementServiceTest  extends Specification implements SpockLabeler {
	
	@Autowired
	IGenericRestClient qmGenericRestClient
	
	@Autowired
	QmProjectManagementService underTest
	
	def 'getProject details.'() {
	
		def testplan = new JsonSlurper().parseText(getClass().getResource('/testdata/projectrepo.json').text)
		1 * qmGenericRestClient.get(_) >> testplan
	
		
		when: w_ 'calling of method under test (getProject)'
		def keyname = underTest.getProject('projectArea')
		
		then: t_ null
		true
		
	}

}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class QmProjectManagementServiceTestConfig {
	def factory = new DetachedMockFactory()
	
	@Bean
	IGenericRestClient qmGenericRestClient() {
		return factory.Mock(ClmGenericRestClient)
	}
	
	@Bean
	QmProjectManagementService underTest() {
		return new QmProjectManagementService()
	}
}
