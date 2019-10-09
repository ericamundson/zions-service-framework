package com.zions.vsts.services.work

import static org.junit.Assert.*

import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cache.MongoDBCacheManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.restart.ICheckpointManagementService
import com.zions.common.services.test.DataGenerationService
import com.zions.common.services.test.SpockLabeler
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
	ICacheManagementService cacheManagementService
	
	@Autowired
	ICheckpointManagementService checkpointManagementService
	
	@Autowired
	DataGenerationService dataGenerationService
	
	def "Injected services are mocks"() {
		expect:
		new MockUtil().isMock(genericRestClient)
	}
	
	
	public void 'Batch call a result'() {
		given: 'stub rateLimitPost a valid return'
		1 * genericRestClient.rateLimitPost(_) >>  [:]
		
		when: 'call tested method'
		underTest.batchWIChanges('stuff', 'aproject', ['stuff'], [:])
		
		then: 'No failure'
		true
	}
	
	public void 'Batch call null result'() {
		given: 'stub rateLimitPost with null return.'
		1 * genericRestClient.rateLimitPost(_) >>  null
		
		when: 'Call method under test with null scenario'
		underTest.batchWIChanges('stuff', 'aproject', ['some stuff'], [:])
		
		then: 'No failure'
		true
	}

	public void 'Batch call with more realistic data'() {
		given: 'stub rateLimitPost with fuller data return'
		1 * genericRestClient.rateLimitPost(_) >>  [value: [[body: "{\"id\":\"123\", \"somejson\": \"morejson\"}", code: 200]]]

		when: 'Call method under test'
		underTest.batchWIChanges('stuff', 'aproject', ['somestuff'], [0: '58879'])
		
		then: 'No exceptions'
		true
	}

	public void 'Batch call with a failed item in batch'() {
		given: 'stub rest call post changes with bad return'
		1 * genericRestClient.rateLimitPost(_) >>  [value: [[body: "{\"value\": {\"Message\": \"Bad juju\"}}", code: 300]]]
		when: 'call batchWIChanges'
		underTest.batchWIChanges('stuff', 'aproject', ['somestuff'], [0: '58879'])
		
		then: 'No exception'
		true
	}

	public void 'refreshCache call with a failed item in batch'() {
		given: 'stub call to get to refreshCache with bad item in batch'
		1 * genericRestClient.get(_) >>  [value: ["{\"id\":\"123\", \"somejson\": \"morejson\"}"]]
		when: 'call refreshCache'
		underTest.refreshCache('stuff', 'aproject', ['58879'])
		
		then: 'No exception'
		true
	}
	
	def 'clean call success flow'() {
		given: 'setup getWorkitems stub'
		def workitems = new JsonSlurper().parseText(this.getClass().getResource('/testdata/workitemsquery.json').text)		
		1 *  genericRestClient.rateLimitPost(_) >> workitems
		1 *  genericRestClient.rateLimitPost(_) >> null
		
		and: 'setup deleteWorkitem stub'
		1* genericRestClient.delete(_) >> null
		
		when: 'call method under test (clean)'
		underTest.clean('', 'theproject', "Select [System.Id], [System.Title] From WorkItems Where ([System.WorkItemType] = 'Test Plan'  OR [System.WorkItemType] = 'Test Case') AND [System.AreaPath] = stuff")

		then: 'nothing to validate'
		true
	}
	
	def 'cleanDuplicates mdb flow'() {
		setup: 'stub for mdb cache management'
		underTest.cacheManagementService = Stub(MongoDBCacheManagementService)
		MongoDBCacheManagementService mdbCacheManagement = underTest.cacheManagementService
		def r = [:]
		mdbCacheManagement.getAllOfType(_, _) >> { args ->
			int page = args[1]
			if (page == 2) {
				return [:]
			} else {
				int i = 22
				0.upto(20, {
					String key = "${i}"
					def wi = dataGenerationService.generate('/testdata/wiDataT.json')
					wi.id = i+200
					wi.fields.'Custom.ExternalID' = key
					r[key] = wi 
					i++
				})
				return r
			}
		}
		
		and: 'stub generic rest client for rateLimitPost that produces a duplicate work item returns to exercise delete'
		underTest.genericRestClient = Stub(GenericRestClient)
		underTest.genericRestClient.rateLimitPost(_) >> { args ->
			def req = args[0]
			String body = "${req.body}"
			def rwi = null
			def result = [workItems:[]]
			r.each { key, wi ->
				if (body.contains("[Custom.ExternalID] = '${key}'")) {
					rwi = wi
					return
				}
			}
			result.workItems.add(rwi)
			
			// setup duplicate
			if (rwi.id == 200 + 30) {
				def wi = dataGenerationService.generate('/testdata/wiDataT.json')
				wi.id = 500
				wi.fields.'Custom.ExternalID' = rwi.fields.'Custom.ExternalID'
				result.workItems.add(wi)
			}
			return result
		}
		
		and: 'stub genericRestClient for do nothing delete'
		underTest.genericRestClient.delete(_) >> { args ->
			return []
		}
				
		when: 'call cleanDuplicates'
		boolean success = true
		try {
			def result = underTest.cleanDuplicates('', 'Dummy')
		} catch (e) { 
			e.printStackTrace()
			success = false 
		}
		
		then: 'No exception'
		success
		
		cleanup: 'Set updated classes members back'
		underTest.genericRestClient = genericRestClient
		underTest.cacheManagementService = cacheManagementService

	}

}

@TestConfiguration
@Profile("test")
@ComponentScan(["com.zions.common.services.restart", "com.zions.common.services.test"])
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
	ICacheManagementService cacheManagementService() {
		return new CacheManagementService(cacheLocation)
	}
	

}

//@Configuration
//@PropertySource("classpath:test.properties")

