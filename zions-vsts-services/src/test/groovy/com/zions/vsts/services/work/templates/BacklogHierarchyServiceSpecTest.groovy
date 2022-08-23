package com.zions.vsts.services.work.templates

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.SpockLabeler
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient
import groovy.json.JsonSlurper
import spock.lang.Ignore
import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[BacklogHierarchyServiceSpecTestConfig])
class BacklogHierarchyServiceSpecTest extends Specification {
	
	@Autowired
	@Value('${test.mapping.file}')
	String testMappingFileName
	
	@Value('${test.work.item.file}')
	String testWorkItemFileName
	
	
	@Autowired
	IGenericRestClient genericRestClient
	
	@Autowired
	ProjectManagementService projectManagementService
	
	@Autowired
	ProcessTemplateService processTemplateService
		
	@Autowired
	BacklogHierarchyService underTest

	def 'getCategoryLevelMap success flow.'() {
		given: 'process template management service getProcessConfiguration stub'
		def processConfigData = new JsonSlurper().parseText(this.getClass().getResource('/testdata/processConfiguration.json').text)
		genericRestClient.get(_) >> processConfigData
		
		when: 'call method (getCategoryLevelMap) under test.'
		def categoryLevelMap = underTest.getCategoryLevelMap().toString()
		
		then: 'should have valid map'
		categoryLevelMap == '[LevelOne:1, PortfolioEpic:2, Epics:3, Features:4, Stories:5, Tasks:6]'
	}
	
	def 'getWitCategoryMap success flow.'() {
		given: 'process template management service getProcessConfiguration stub'
		def processConfigData = new JsonSlurper().parseText(this.getClass().getResource('/testdata/processConfiguration.json').text)
		genericRestClient.get(_) >> processConfigData
		
		when: 'call method (getWitCategoryMap) under test.'
		def witCategoryMap = underTest.getWitCategoryMap().toString()
		
		then: 'should have valid map'
		witCategoryMap == '[LevelOne:LevelOne, Portfolio Epic:PortfolioEpic, Epic:Epics, LPM Task:Epics, AP Request:Epics, Component:Epics, Feature:Features, Sub Component:Features, DAG:Features, User Story:Stories, Impediment:Stories, Idea:Stories, PEN:Stories, Change Request:Stories, Spike:Stories, Bug:Stories, Task:Tasks]'
	}
	
}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class BacklogHierarchyServiceSpecTestConfig {
	def mockFactory = new DetachedMockFactory()

	@Bean
	BacklogHierarchyService underTest() {
		return new BacklogHierarchyService()
	}

	@Bean
	ProjectManagementService projectManagementService() {
		return new ProjectManagementService()
	}
	
	@Bean
	ProcessTemplateService processTemplateService() {
		return new ProcessTemplateService()
	}
	
	@Bean
	IGenericRestClient genericRestClient() {
		return mockFactory.Stub(GenericRestClient)
	}
}