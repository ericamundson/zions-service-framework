package com.zions.vsts.services.work.templates

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient
import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[ProcessTemplateServiceSpecTestConfig])
class ProcessTemplateServiceSpecTest extends Specification {
	
	@Autowired
	IGenericRestClient genericRestClient
	
	@Autowired
	ProjectManagementService projectManagementService
	
	@Autowired
	ProcessTemplateService underTest

	def 'getTypeMapResource bad map file.'() {
		given: 'No stubs'
		
		when: 'call method (getTypeMapResource) under test.'
		def maps = underTest.getTypeMapResource('bad.json')
		
		then: ''
		maps.size() == 0
	}
	
	def 'getTypeMapResource success flow.'() {
		given: 'No stubs'
		
		when: 'call method (getTypeMapResource) under test.'
		def maps = underTest.getTypeMapResource('rtctypemap.json')
		
		then: ''
		maps.size() == 7
	}

	def 'getNameMapResource bad map file.'() {
		given: 'No stubs'
		
		when: 'call method (getNameMapResource) under test.'
		def maps = underTest.getNameMapResource('bad.json')
		
		then: ''
		maps.size() == 0
	}
	

	def 'getWorkitems success flow.'() {
		given: 'project management service getProjecProperty stub'
		1 * projectManagementService.getProjectProperty(_, _ , _) >> "aTemplateId"
		
		and: 'azure rest client http get stub'
		def rwits = new JsonSlurper().parseText(getClass().getResource('/testdata/workitemtypes.json').text)
		1 * genericRestClient.get(_) >> rwits
		
		when: 'call method (getWorkitems) under test.'
		def wits = underTest.getWorkItemTypes('', 'project')
		
		then: ''
		wits.size() > 0
	}

}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class ProcessTemplateServiceSpecTestConfig {
	def mockFactory = new DetachedMockFactory()
	
	@Bean
	IGenericRestClient genericRestClient() {
		return mockFactory.Mock(GenericRestClient, name: 'genericRestClient')
	}

	@Bean
	ProjectManagementService projectManagementService() {
		return mockFactory.Mock(ProjectManagementService)
	}
	
	@Bean
	ProcessTemplateService underTest() {
		return new ProcessTemplateService()
	}
}