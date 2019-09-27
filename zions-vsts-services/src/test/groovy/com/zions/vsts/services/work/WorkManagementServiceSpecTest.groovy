package com.zions.vsts.services.work

import static org.junit.Assert.*

import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.restart.ICheckpointManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient
import groovy.json.JsonSlurper
import org.junit.Test
import org.junit.runner.RunWith
import org.spockframework.mock.MockUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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
	
	@Autowired
	ICacheManagementService cacheManagmentService
	
	@Autowired
	ICheckpointManagementService checkpointManagementService
	
	def "Injected services are mocks"() {
		expect:
		new MockUtil().isMock(genericRestClient)
	}
	
	public void 'Batch call a result'() {
		given: g_ 'stub rateLimitPost a valid return'
		1 * genericRestClient.rateLimitPost(_) >>  [:]
		
		when: w_ 'call tested method'
		underTest.batchWIChanges('stuff', 'aproject', ['stuff'], [:])
		
		then: t_ 'No failure'
		true
	}
	
	public void 'Batch call null result'() {
		given: g_ 'stub rateLimitPost with null return.'
		1 * genericRestClient.rateLimitPost(_) >>  null
		
		when: w_ 'Call method under test with null scenario'
		underTest.batchWIChanges('stuff', 'aproject', ['some stuff'], [:])
		
		then: t_ 'No failure'
		true
	}

	public void 'Batch call with more realistic data'() {
		given: g_ 'stub rateLimitPost with fuller data return'
		1 * genericRestClient.rateLimitPost(_) >>  [value: [[body: "{\"id\":\"123\", \"somejson\": \"morejson\"}", code: 200]]]

		when: w_ 'Call method under test'
		underTest.batchWIChanges('stuff', 'aproject', ['somestuff'], [0: '58879'])
		
		then: t_ 'No exceptions'
		true
	}

	public void 'Batch call with a failed item in batch'() {
		given: g_ 'stub rest call post changes with bad return'
		1 * genericRestClient.rateLimitPost(_) >>  [value: [[body: "{\"value\": {\"Message\": \"Bad juju\"}}", code: 300]]]
		when: w_ 'call batchWIChanges'
		underTest.batchWIChanges('stuff', 'aproject', ['somestuff'], [0: '58879'])
		
		then: t_ null
		true
	}

	public void 'refreshCache call with a failed item in batch'() {
		given: g_ 'stub call to get to refreshCache with bad item in batch'
		1 * genericRestClient.get(_) >>  [value: ["{\"id\":\"123\", \"somejson\": \"morejson\"}"]]
		when: w_ 'call refreshCache'
		underTest.refreshCache('stuff', 'aproject', ['58879'])
		
		then: t_ null
		true
	}
	
	def 'clean call success flow'() {
		given: g_ 'setup getWorkitems stub'
		def workitems = new JsonSlurper().parseText(this.getClass().getResource('/testdata/workitemsquery.json').text)		
		1 *  genericRestClient.rateLimitPost(_) >> workitems
		1 *  genericRestClient.rateLimitPost(_) >> null
		
		and: a_ 'setup deleteWorkitem stub'
		1* genericRestClient.delete(_) >> null
		
		when: w_ 'call method under test (clean)'
		underTest.clean('', 'theproject', "Select [System.Id], [System.Title] From WorkItems Where ([System.WorkItemType] = 'Test Plan'  OR [System.WorkItemType] = 'Test Case') AND [System.AreaPath] = stuff")

		then: t_ 'nothing to validate'
		true
	}
	
}

@TestConfiguration
@Profile("test")
@ComponentScan("com.zions.common.services.restart")
@PropertySource("classpath:test.properties")
class WorkManagementServiceConfig {
	def mockFactory = new DetachedMockFactory()
	
	@Autowired
	@Value('${cache.location}')
	String cacheLocation

	@Bean
	IGenericRestClient genericRestClient() {
		return mockFactory.Mock(GenericRestClient, name: 'genericRestClient')
	}
	
	@Bean
	WorkManagementService underTest() {
		WorkManagementService out = new WorkManagementService()
		return out
	}
	
	@Bean
	ICacheManagementService cacheManagmentService() {
		return new CacheManagementService(cacheLocation)
	}
	

}

//@Configuration
//@PropertySource("classpath:test.properties")

