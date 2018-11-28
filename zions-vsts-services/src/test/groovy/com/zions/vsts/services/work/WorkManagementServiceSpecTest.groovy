package com.zions.vsts.services.work

import static org.junit.Assert.*

import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient
import org.junit.Test
import org.junit.runner.RunWith
import org.spockframework.mock.MockUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.stereotype.Component
import org.springframework.test.context.BootstrapWith
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.context.support.AnnotationConfigContextLoader
import org.springframework.test.context.support.DefaultTestContextBootstrapper
import org.springframework.test.context.web.WebAppConfiguration
import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[WorkManagementServiceConfig])
class WorkManagementServiceSpecTest extends Specification {

	@Autowired
	WorkManagementService underTest
		
	@Autowired
	IGenericRestClient genericRestClient
	
	def "Injected services are mocks"() {
		expect:
		new MockUtil().isMock(genericRestClient)
	}
	
	public void 'Batch call a result'() {
		given: 'stub rateLimitPost a valid return'
		1 * genericRestClient.rateLimitPost(_) >>  [:]
		
		when: 'call tested method'
		underTest.batchWIChanges('stuff', 'aproject', ['stuff'], [:])
		
		then:
		true
	}
	
	public void 'Batch call null result'() {
		given: 'stub rateLimitPost with null return.'
		1 * genericRestClient.rateLimitPost(_) >>  null
		
		when: 'Call method under test with null scenario'
		underTest.batchWIChanges('stuff', 'aproject', ['some stuff'], [:])
		
		then:
		true
	}

	public void 'Batch call with more realistic data'() {
		given: 'stub rateLimitPost with fuller data return'
		1 * genericRestClient.rateLimitPost(_) >>  [value: [[body: "{\"id\":\"123\", \"somejson\": \"morejson\"}", code: 200]]]

		when: 'Call method under test'
		underTest.batchWIChanges('stuff', 'aproject', ['somestuff'], [0: '58879'])
		
		then:
		true
	}

	public void 'Batch call with a failed item in batch'() {
		given:
		1 * genericRestClient.rateLimitPost(_) >>  [value: [[body: "{\"value\": {\"Message\": \"Bad juju\"}}", code: 300]]]
		when:
		underTest.batchWIChanges('stuff', 'aproject', ['somestuff'], [0: '58879'])
		
		then:
		true
	}

	public void 'refreshCache call with a failed item in batch'() {
		given:
		1 * genericRestClient.get(_) >>  [value: ["{\"id\":\"123\", \"somejson\": \"morejson\"}"]]
		when:
		underTest.refreshCache('stuff', 'aproject', ['58879'])
		
		then:
		true
	}
}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class WorkManagementServiceConfig {
	def mockFactory = new DetachedMockFactory()
	
	@Bean
	IGenericRestClient genericRestClient() {
		return mockFactory.Mock(GenericRestClient, name: 'genericRestClient')
	}
	
	@Bean
	WorkManagementService underTest() {
		WorkManagementService out = new WorkManagementService()
		return out
	}
	

}

//@Configuration
//@PropertySource("classpath:test.properties")

