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
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient
import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[ProcessTemplateServiceSpecTestConfig])
class ProcessTemplateServiceSpecTest extends Specification {
	
	@Autowired
	@Value('${test.mapping.file}')
	String testMappingFileName
	
	
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
	
	def 'getWorkItemType success flow.'() {
		given: 'project management service getProjecProperty stub'
		1 * projectManagementService.getProjectProperty(_, _ , _) >> "aTemplateId"
		
		and: 'azure rest client http get stub'
		def out = new JsonSlurper().parseText(getClass().getResource('/testdata/processDTSTest.json').text)
		1 * genericRestClient.get(_) >> out
		
		and:
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when: 'call method (getWorkItemType) under test.'
		def result = underTest.getWorkItemType('', 'DigitalBanking',  'DTSTest.EnhancementRequest','none','System.ProcessTemplateType')
		
		then: ''
		"${result.name}" == "Enhancement Request"
	}
	
	def 'getWorkitemTemplateFields success flow.'() {
		given: 'project management service getProject stub'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		1 * projectManagementService.getProject(_, _ ) >> project
		
		and: 'azure rest client http get stub'
		def out = new JsonSlurper().parseText(getClass().getResource('/testdata/dfprocessfields.json').text)
		1 * genericRestClient.get(_) >> out
		
		when: 'call method (getWorkitemTemplateFields) under test.'
		def result = underTest.getWorkitemTemplateFields('',  '',  '')
		
		then: ''
		"${result.count}" == "58"
	}
	
	/*def 'getWorkitemTemplateXML success flow.'() {
		
		when: 'call method (getWorkitemTemplateXML) under test.'
		def result = underTest.getWorkitemTemplateXML('',  '',  '')
		
		then: ''
		result == null
	}
	*/
	def 'getField success flow.'() {
		given: 
		String json = this.getClass().getResource('/testdata/repos.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		String url = "https://dev.azure.com/eto-dev/DigitalBanking/_apis/git/repositories/"
		
		when: 'call method (getField) under test.'
		def result = underTest.getField(url)
		
		then: ''
		"${result.count}" == "4"
	}
	
	def 'getFields success flow.'() {
		given:
		String json = this.getClass().getResource('/testdata/processfields.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when: 'call method (getFields) under test.'
		def result = underTest.getFields('', project)
		
		then: ''
		"${result.count}" == "220"
	}
	
	def 'queryForField success flow.'() {
		given:
		String json = this.getClass().getResource('/testdata/processfields.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when: 'call method (queryForField) under test.'
		def result = underTest.queryForField('', project, '', true)
		
		then: ''
		result == null
	}
	
	def 'getField success flow with three params'() {
		given:
		given: 'project management service getProject stub'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		1 * projectManagementService.getProject(_, _ ) >> project
		
		and:
		String json = this.getClass().getResource('/testdata/Microsoft.VSTS.Common.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		when: 'call method (getField) under test.'
		def result = underTest.getField('', 'DigitalBanking', '')
		
		then: ''
		"${result.name}" == "Acceptance Criteria"
	}
	
	def 'updateWorkitemTemplate success flow with three params'() {
		given:
		String json = this.getClass().getResource('/testdata/processbug.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.put(_) >> out
		
		when: 'call method (updateWorkitemTemplate) under test.'
		def result = underTest.updateWorkitemTemplate('', 'DigitalBanking', '', '')
		
		then: ''
		"${result.name}" == "Bug"
	}
	
	def 'getDefaultMapping success flow '() {
		given:
		def mappingDataInfo = []
		def xmlMappingData = new XmlSlurper().parse(new File(testMappingFileName))
		xmlMappingData.wit.each { tType ->
			def map = [source: tType.@source, target: tType.@target, fields: []]
			tType.field.each { field ->
				def ofield = [source: field.@source, target: field.@target, defaultValue: null, values:[]]
				field.'value'.each { aValue ->
					def oValue = [source: aValue.@source, target: aValue.@target]
					ofield.values.add(oValue) 
				}
				field.defaultvalue.each { dValue ->
					ofield.defaultValue = dValue.@target
				}
				map.fields.add(ofield)
			}
			mappingDataInfo.add(map)
		}
		
		def mapping = [mappingDataInfo]
		
		when: 'call method (getDefaultMapping) under test.'
		def result = underTest.getDefaultMapping(mapping)
		
		then: ''
		result != null;
	}
	
	def 'hasMapping success flow with three params'() {
		given:
		def mapping =[:]
		def wit =[:]
		
		when: 'call method (hasMapping) under test.'
		def result = underTest.hasMapping(mapping, wit)
		
		then: ''
		result == false;
	}
	
	def 'createPickList success flow'() {
		given:
		String json = this.getClass().getResource('/testdata/processlists.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.post(_) >> out
		
		and:
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def witFieldChange= [:]
		
		when: 'call method (createPickList) under test.'
		def result = underTest.createPickList('', project, witFieldChange)
		
		then: ''
		"${result.count}" == "15"
	}
	
	def 'genColor success flow'() {
		
		when: 'call method (genColor) under test.'
		def result = underTest.genColor()
		
		then: ''
		result != null
	}
	
	/*def 'ensureLayout success flow'() {
		given:
		String json = this.getClass().getResource('/testdata/processlists.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.post(_) >> out
		
		and:
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def wit= [:]
		def defaultWit = [:]
		def layout = [:]
		def pages = [:] 
		layout << pages
		defaultWit << layout
		
		when: 'call method (ensureLayout) under test.'
		//def result = underTest.ensureLayout('', project, wit, defaultWit)
		
		then: ''
		//result != null
	}*/
	
	def 'updateWorkitemTemplates success flow.'() {
		given: 'No stubs'
		def mapping = underTest.getTypeMapResource('rtctypemap.json')
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		def ccmWits
		
		when:
		def result = underTest.updateWorkitemTemplates('', project, mapping, ccmWits)
		
		then: ''
		result != null
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