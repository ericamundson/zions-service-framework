package com.zions.vsts.services.work.calculations

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration
import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.DataGenerationService
import com.zions.vsts.services.settings.SettingsManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient
import com.zions.vsts.services.work.WorkManagementService
import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[RollupManagementServiceSpecConfig])
class RollupManagementServiceSpec extends Specification {
	@Autowired
	RollupManagementService underTest
	
	@Autowired
	DataGenerationService dataGenerationService
	
	@Autowired
	IGenericRestClient genericRestClient
	
	@Autowired
	WorkManagementService workManagementService

	def 'rollup happy path'() {
		given: g_ 'stub of get work item of feature type'
		1 * workManagementService.getWorkItem(_, _, _) >> dataGenerationService.generate('/testdata/wiDataFeature.json')
		
		and: a_ 'stub of get category'
		1 * workManagementService.getCategory(_,_,_) >> 'Feature Category'
		
		and: a_ 'stub of inside get child data, get children of Feature  (a story)'
		def storyData = dataGenerationService.generate('/testdata/wiDataUserStory.json')
		1 * workManagementService.getChildren(_,_,_) >> [storyData]
		
		1 * workManagementService.getCategory(_,_,_) >> 'Requirement Category'
				
		and: a_ 'stub of get work item of story type'
		1 * workManagementService.getWorkItem(_, _, _) >> storyData
		
		and: a_ 'stub of get category for story'
		1 * workManagementService.getCategory(_,_,_) >> 'Requirement Category'
		
		and: a_ 'stub of inside get child data, get children of story (Tasks)'
		def tasks = []
		for (int i = 0; i < 3; i++) {
			def taskData = dataGenerationService.generate('/testdata/wiDataTask.json')
			tasks.push(taskData)
		}
		1 * workManagementService.getChildren(_,_,_) >> tasks
		
		3 * workManagementService.getCategory(_,_,_) >> 'Task Category'
		
		and: a_ 'stub of get work item of story type'
		1 * workManagementService.getWorkItem(_, _, _) >> storyData

		and: a_ 'stub save story and feature'
		2 * workManagementService.updateWorkItem(_,_,_,_)
		
		when: w_ 'Call underTest (rollup)'
		boolean flag = true
		try {
			underTest.rollup('1234', false, '')
		} catch (e) {
			flag = false
		}
		
		then: t_ null
		flag
		
	}

}

@TestConfiguration
@Profile("test")
@ComponentScan(["com.zions.common.services.test"])
@PropertySource("classpath:test.properties")
class RollupManagementServiceSpecConfig {
	def mockFactory = new DetachedMockFactory()
	
	@Autowired
	@Value('${cache.location}')
	String cacheLocation
	
	@Bean
	IGenericRestClient genericRestClient() {
		return mockFactory.Mock(GenericRestClient)
	}

	@Bean
	WorkManagementService workManagementService() {
		return mockFactory.Mock(WorkManagementService)
	}
	@Bean
	SettingsManagementService settingsManagementService() {
		return mockFactory.Mock(SettingsManagementService)
	}

	@Bean
	RollupManagementService underTest() {
		return new RollupManagementService()
	}

	@Bean
	DataGenerationService dataGenerationService() {
		return new DataGenerationService()
	}
	
	@Bean
	ICacheManagementService cacheManagementService() {
		return new CacheManagementService()
	}
}
