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

import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.DataGenerationService
import com.zions.vsts.services.settings.SettingsManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient
import com.zions.vsts.services.work.WorkManagementService
import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[RollupManagementServiceSpecConfig])
class RollupManagementServiceSpec extends Specification {

	def 'rollup happy path'() {
		given: 'stub of get work item of feature type'
		
		and: 'stub of get category'
		
		and: 'stub of inside get child data, get children of Feature  (a story)'
		
		and: 'inside child rollup of story'
		
		and: 'stub of get work item of story type'
		
		and: 'stub of get category for story'
		
		and: 'stub of inside get child data, get children of story (Tasks)'
		
		when: ''
		
		then:
		true
		
	}

}

@TestConfiguration
@Profile("test")
@ComponentScan(["com.zions.common.services.test"])
@PropertySource("classpath:test.properties")
class RollupManagementServiceSpecConfig {
//	def mockFactory = new DetachedMockFactory()
//	
//	@Autowired
//	@Value('${cache.location}')
//	String cacheLocation
//	
//	@Bean
//	IGenericRestClient genericRestClient() {
//		return mockFactory.Mock(GenericRestClient)
//	}
//
//	@Bean
//	WorkManagementService workManagementService() {
//		return mockFactory.Mock(WorkManagementService)
//	}
//	@Bean
//	SettingsManagementService settingsManagementService() {
//		return mockFactory.Mock(SettingsManagementService)
//	}
//
//	@Bean
//	RollupManagementService underTest() {
//		return new RollupManagementService()
//	}
//
//	@Bean
//	DataGenerationService dataGenerationService() {
//		return new DataGenerationService()
//	}
}
