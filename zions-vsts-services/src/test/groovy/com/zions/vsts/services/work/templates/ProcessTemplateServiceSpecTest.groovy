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
	
	@Value('${test.work.item.file}')
	String testWorkItemFileName
	
	
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
		def xmlMappingData = new XmlSlurper().parse(new File(testMappingFileName))
		
		when: 'call method (getDefaultMapping) under test.'
		def result = underTest.getDefaultMapping(xmlMappingData)
		
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
	
	def 'updateWorkitemTemplates exception flow.'() {
		given: 'No stubs'
		def mapping = underTest.getTypeMapResource('rtctypemap.json')
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		def ccmWits = new XmlSlurper().parse(new File(testWorkItemFileName))
		
		when:
		def result = underTest.updateWorkitemTemplates('', project, mapping, ccmWits)
		
		then: ''
		thrown NullPointerException
	}
	
	def 'getWITMapping success flow.'() {
		given: 'No stubs'
		def mapping = new XmlSlurper().parse(new File(testMappingFileName))
		
		def wits = new XmlSlurper().parse(new File(testWorkItemFileName))
		
		when:
		def result = underTest.getWITMapping(mapping, wits)
		
		then: ''
		true
	}
	
	def 'ensureWITChanges success flow.'() {
		given: 'No stubs'
		def mapping = new XmlSlurper().parse(new File(testMappingFileName))
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def changes = []
		def change = [id: 1, displayName: 'abc', from: 'a', to: 'b']
		changes.add(change)
		when:
		def result = underTest.ensureWITChanges('', project, '')
		
		then: ''
		true
	}
	
	def loadCCMWITs() {
		def wits = []
		URI uri = this.getClass().getResource('/testdata/wit_templates').toURI()
		File tDir = new File(uri.path)
		if (tDir.exists() || tDir.isDirectory()) {
			tDir.eachFile { file ->
				def witData = new XmlSlurper().parse(file)
				wits.add(witData)
			}
		}
		return wits
	}
	
	@Test
	def 'getTranslateMapping success flow one' () {
		given:
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def mapping = new XmlSlurper().parse(new File(testMappingFileName))
		def ccmWits = new XmlSlurper().parse(new File('./src/test/resources/testdata/workitems.xml'))
		
		and:
		String json = this.getClass().getResource('/testdata/processfields.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		when:
		def result = underTest.getTranslateMapping('', project, mapping, ccmWits)
		
		then:
		true
	}
	
	@Test
	def 'getTranslateMapping success flow two' () {
		given:
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def mapping = new XmlSlurper().parse(new File(testMappingFileName))
		def ccmWits = new XmlSlurper().parse(new File('./src/test/resources/testdata/workitems.xml'))
		
		and:
		String json = this.getClass().getResource('/testdata/processfields.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		when:
		def result = underTest.getTranslateMapping('', project, mapping, ccmWits)
		
		then:
		true
	}

	@Test
	def 'getLinkMapping success flow' () {
		given:
		def mapping = new XmlSlurper().parse(new File(testMappingFileName))
		
		when:
		def result = underTest.getLinkMapping(mapping)
		
		then:
		result != null
	}
	
	@Test
	def 'getWIT success flow' () {
		given:
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def changes = new XmlSlurper().parse(new File('./src/test/resources/testdata/workitems.xml'))
		def rwits = new JsonSlurper().parseText(getClass().getResource('/testdata/workitemtypes.json').text)
		genericRestClient.get(_) >> rwits
		
		when:
		def resultOne = underTest.getWIT('eto-dev', 'DigitalBanking', 'Enhancement Request')
		def resultTwo = underTest.getWIT('eto-dev', 'DigitalBanking', 'Something')
		
		then:
		resultOne == null
		resultTwo == null
	}
	
	@Test
	def 'ensureWitFieldLayout success flow' () {
		given:
		def wit = new XmlSlurper().parse(new File('./src/test/resources/testdata/workitems.xml'))
		def layout = new JsonSlurper().parseText(getClass().getResource('/testdata/layout.json').text)
		genericRestClient.get(_) >> layout
		
		def pages = new JsonSlurper().parseText(getClass().getResource('/testdata/page.json').text)
		genericRestClient.post(_) >> pages
		
		def witFieldChange = new JsonSlurper().parseText(getClass().getResource('/testdata/witfieldchange.json').text)
		def field = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		//new XmlSlurper().parse(new File('./src/test/resources/testdata/witfieldchange.xml'))
		
		when:
		def result = underTest.ensureWitFieldLayout('', 'DigitalBanking', wit, field, witFieldChange)
		
		then:
		result == null
	}	
	
	@Test
	def 'createWITGroup success flow' () {
		given:
		def out = new JsonSlurper().parseText(getClass().getResource('/testdata/layout.json').text)
		genericRestClient.post(_) >> out
		
		def wit = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		def externalPage = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		
		when:
		def result = underTest.createWITGroup('', '', wit, externalPage, '', '')
		
		then:
		result != null
	}
	
	@Test
	def 'createWITPage success flow' () {
		given:
		def out = new JsonSlurper().parseText(getClass().getResource('/testdata/layout.json').text)
		genericRestClient.post(_) >> out
		
		def wit = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		
		when:
		def result = underTest.createWITPage('', '', wit, '')
		
		then:
		result != null
	}
	
	@Test
	def 'createField success flow' () {
		given:
		def out = new JsonSlurper().parseText(getClass().getResource('/testdata/layout.json').text)
		genericRestClient.post(_) >> out
		
		def witFieldChange = new JsonSlurper().parseText(getClass().getResource('/testdata/witfieldchange.json').text)
		
		when:
		def result = underTest.createField('', '', witFieldChange, '')
		
		then:
		result != null
	}
	
	@Test
	def 'addGroup success flow' () {
		given:
		def out = new JsonSlurper().parseText(getClass().getResource('/testdata/layout.json').text)
		genericRestClient.post(_) >> out
		
		def wit = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		
		when:
		def result = underTest.addGroup('', '', wit, 123, 12, 'gName')
		
		then:
		result != null
	}
	
	@Test
	def 'addControl success flow' () {
		given:
		def out = new JsonSlurper().parseText(getClass().getResource('/testdata/layout.json').text)
		genericRestClient.post(_) >> out
		
		def wit = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		def controlData = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		
		when:
		def result = underTest.addControl('', '', wit, 123, controlData)
		
		then:
		result == null
	}
	
	@Test
	def 'addWITField success flow' () {
		given:
		def out = new JsonSlurper().parseText(getClass().getResource('/testdata/layout.json').text)
		genericRestClient.post(_) >> out
		
		def wit = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		def controlData = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		
		when:
		def result = underTest.addWITField('', '', 'wrefName', '') 
		
		then:
		result != null
	}
	
	@Test
	def 'getWITField success flow' () {
		given:
		def out = new JsonSlurper().parseText(getClass().getResource('/testdata/layout.json').text)
		genericRestClient.post(_) >> out
		
		def wit = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		def field = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		
		when:
		def result = underTest.getWITField('', '', wit, field)
		
		then:
		result == null
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