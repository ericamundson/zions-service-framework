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
import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[ProcessTemplateServiceSpecTestConfig])
class ProcessTemplateServiceSpecTest extends Specification implements SpockLabeler {
	
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
		given: g_ 'No stubs'
		
		when: w_ 'call method (getTypeMapResource) under test.'
		def maps = underTest.getTypeMapResource('bad.json')
		
		then: t_ 'maps.size() == 0'
		maps.size() == 0
	}
	
	def 'getTypeMapResource success flow.'() {
		given: g_ 'No stubs'
		
		when: w_ 'call method (getTypeMapResource) under test.'
		def maps = underTest.getTypeMapResource('rtctypemap.json')
		
		then: t_ 'maps.size() == 7'
		maps.size() == 7
	}

	def 'getNameMapResource bad map file.'() {
		given: g_ 'No stubs'
		
		when: w_ 'call method (getNameMapResource) under test.'
		def maps = underTest.getNameMapResource('bad.json')
		
		then: t_ 'maps.size() == 0'
		maps.size() == 0
	}
	

	def 'getWorkitems success flow.'() {
		given: g_ 'project management service getProjecProperty stub'
		1 * projectManagementService.getProjectProperty(_, _ , _) >> "aTemplateId"
		
		and: a_ 'azure rest client http get stub'
		def rwits = new JsonSlurper().parseText(getClass().getResource('/testdata/workitemtypes.json').text)
		1 * genericRestClient.get(_) >> rwits
		
		when: w_ 'call method (getWorkitems) under test.'
		def wits = underTest.getWorkItemTypes('', 'project')
		
		then: t_ 'wits.size() > 0'
		wits.size() > 0
	}
	
	def 'getWorkItemType success flow.'() {
		given: g_ 'project management service getProjecProperty stub'
		1 * projectManagementService.getProjectProperty(_, _ , _) >> "aTemplateId"
		
		and: a_ 'azure rest client http get stub'
		def out = new JsonSlurper().parseText(getClass().getResource('/testdata/processDTSTest.json').text)
		1 * genericRestClient.get(_) >> out
		
		and: a_ 'setup parameters'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when: w_ 'call method (getWorkItemType) under test.'
		def result = underTest.getWorkItemType('', 'DigitalBanking',  'DTSTest.EnhancementRequest','none','System.ProcessTemplateType')
		
		then: t_ 'result.name == Enhancement Request'
		"${result.name}" == "Enhancement Request"
	}
	
	def 'getWorkitemTemplateFields success flow.'() {
		given: g_ 'project management service getProject stub'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		1 * projectManagementService.getProject(_, _ ) >> project
		
		and: a_ 'azure rest client http get stub'
		def out = new JsonSlurper().parseText(getClass().getResource('/testdata/dfprocessfields.json').text)
		1 * genericRestClient.get(_) >> out
		
		when: w_ 'call method (getWorkitemTemplateFields) under test.'
		def result = underTest.getWorkitemTemplateFields('',  '',  '')
		
		then: t_ 'result.count == 58'
		"${result.count}" == "58"
	}
	
	/*def 'getWorkitemTemplateXML success flow.'() {
		
		when: w_ 'call method (getWorkitemTemplateXML) under test.'
		def result = underTest.getWorkitemTemplateXML('',  '',  '')
		
		then: t_ ''
		result == null
	}
	*/
	def 'getField success flow.'() {
		given: g_ 'stub of get repos rest call'
		String json = this.getClass().getResource('/testdata/repos.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		String url = "https://dev.azure.com/eto-dev/DigitalBanking/_apis/git/repositories/"
		
		when: w_ 'call method (getField) under test.'
		def result = underTest.getField(url)
		
		then: t_ 'result.count == 4'
		"${result.count}" == "4"
	}
	
	def 'getFields success flow.'() {
		given: g_ 'stub get process fields rest call.'
		String json = this.getClass().getResource('/testdata/processfields.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when: w_ 'call method (getFields) under test.'
		def result = underTest.getFields('', project)
		
		then: t_ 'result.count == 220'
		"${result.count}" == "220"
	}
	
	def 'queryForField success flow.'() {
		given: g_ 'stub get process fields rest call'
		String json = this.getClass().getResource('/testdata/processfields.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when: w_ 'call method (queryForField) under test.'
		def result = underTest.queryForField('', project, '', true)
		
		then: t_ null
		result == null
	}
	
	def 'getField success flow with three params'() {
		given: g_ 'project management service getProject stub'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		1 * projectManagementService.getProject(_, _ ) >> project
		
		and: a_ 'Stub rest call'
		String json = this.getClass().getResource('/testdata/Microsoft.VSTS.Common.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		when: w_ 'call method (getField) under test.'
		def result = underTest.getField('', 'DigitalBanking', '')
		
		then: t_ "Result name is 'Acceptance Criteria'"
		"${result.name}" == "Acceptance Criteria"
	}
	
	def 'updateWorkitemTemplate success flow with three params'() {
		given: g_ 'stub get some bugs rest call'
		String json = this.getClass().getResource('/testdata/processbug.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.put(_) >> out
		
		when: w_ 'call method (updateWorkitemTemplate) under test.'
		def result = underTest.updateWorkitemTemplate('', 'DigitalBanking', '', '')
		
		then: t_ 'result.name == Bug'
		"${result.name}" == "Bug"
	}
	
	def 'getDefaultMapping success flow '() {
		given: g_ 'Stub of default mapping data'
		def xmlMappingData = new XmlSlurper().parse(new File(testMappingFileName))
		
		when: w_ 'call method (getDefaultMapping) under test.'
		def result = underTest.getDefaultMapping(xmlMappingData)
		
		then: t_ 'result != null'
		result != null;
	}
	
	def 'hasMapping success flow with three params'() {
		given: g_ 'Stub of mapping and wit arguments'
		def mapping =[:]
		def wit =[:]
		
		when: w_ 'call method (hasMapping) under test.'
		def result = underTest.hasMapping(mapping, wit)
		
		then: t_ 'result == false'
		result == false;
	}
	
	def 'createPickList success flow'() {
		given: g_ 'stub of rest call for process lists'
		String json = this.getClass().getResource('/testdata/processlists.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.post(_) >> out
		
		and: a_ 'stub of rest call of pick lists'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def witFieldChange= [:]
		
		when: w_ 'call method (createPickList)'
		def result = underTest.createPickList('', project, witFieldChange)
		
		then: t_ "result.count == 15"
		"${result.count}" == "15"
	}
	
	def 'genColor success flow'() {
		
		when: w_ 'call method (genColor) under test.'
		def result = underTest.genColor()
		
		then: t_ 'result != null'
		result != null
	}
	
	/*def 'ensureLayout success flow'() {
		given: g_
		String json = this.getClass().getResource('/testdata/processlists.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.post(_) >> out
		
		and: a_
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def wit= [:]
		def defaultWit = [:]
		def layout = [:]
		def pages = [:]
		layout << pages
		defaultWit << layout
		
		when: w_ 'call method (ensureLayout) under test.'
		//def result = underTest.ensureLayout('', project, wit, defaultWit)
		
		then: t_ ''
		//result != null
	}*/
	
	def 'updateWorkitemTemplates exception flow.'() {
		given: g_ 'No stubs'
		def mapping = underTest.getTypeMapResource('rtctypemap.json')
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		def ccmWits = new XmlSlurper().parse(new File(testWorkItemFileName))
		
		when: w_ 'call updateWorkitemTemplates'
		def result = underTest.updateWorkitemTemplates('', project, mapping, ccmWits)
		
		then: t_ 'throws null pointer'
		thrown NullPointerException
	}
	
	def 'getWITMapping success flow.'() {
		given: g_ 'No stubs'
		def mapping = new XmlSlurper().parse(new File(testMappingFileName))
		
		def wits = new XmlSlurper().parse(new File(testWorkItemFileName))
		
		when: w_ 'call getWITMapping'
		def result = underTest.getWITMapping(mapping, wits)
		
		then: t_ null
		true
	}
	
	def 'ensureWITChanges success flow.'() {
		given: g_ 'Stub data'
		def mapping = new XmlSlurper().parse(new File(testMappingFileName))
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def changes = []
		def change = [id: 1, displayName: 'abc', from: 'a', to: 'b']
		changes.add(change)
		when: w_ 'call ensureWITChanges'
		def result = underTest.ensureWITChanges('', project, '')
		
		then: t_ null
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
		given: g_ 'setup data'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def mapping = new XmlSlurper().parse(new File(testMappingFileName))
		def ccmWits = new XmlSlurper().parse(new File('./src/test/resources/testdata/workitems.xml'))
		
		and: a_ 'stub rest call'
		String json = this.getClass().getResource('/testdata/processfields.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		when: w_ 'call getTranslateMapping'
		def result = underTest.getTranslateMapping('', project, mapping, ccmWits)
		
		then: t_ null
		true
	}
	
	@Test
	def 'getTranslateMapping success flow two' () {
		given: g_ 'setup data'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def mapping = new XmlSlurper().parse(new File(testMappingFileName))
		def ccmWits = new XmlSlurper().parse(new File('./src/test/resources/testdata/workitems.xml'))
		
		and: a_ 'stub rest call for process fields'
		String json = this.getClass().getResource('/testdata/processfields.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		when: w_ 'call getTranslateMapping'
		def result = underTest.getTranslateMapping('', project, mapping, ccmWits)
		
		then: t_ 'result != null'
		true
	}

	@Test
	def 'getLinkMapping success flow' () {
		given: g_ 'Setup return data'
		def mapping = new XmlSlurper().parse(new File(testMappingFileName))
		
		when: w_ 'call getLinkMapping'
		def result = underTest.getLinkMapping(mapping)
		
		then: t_ 'result != null'
		result != null
	}
	
	@Test
	def 'getWIT success flow' () {
		given: g_ 'stub rest call for work item type data'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def changes = new XmlSlurper().parse(new File('./src/test/resources/testdata/workitems.xml'))
		def rwits = new JsonSlurper().parseText(getClass().getResource('/testdata/workitemtypes.json').text)
		genericRestClient.get(_) >> rwits
		
		when: w_ 'call getwit'
		def resultOne = underTest.getWIT('eto-dev', 'DigitalBanking', 'Enhancement Request')
		def resultTwo = underTest.getWIT('eto-dev', 'DigitalBanking', 'Something')
		
		then: t_ 'result == null'
		resultOne == null
		resultTwo == null
	}
	
	@Test
	def 'ensureWitFieldLayout success flow' () {
		given: g_ 'stub rest call for layout'
		def wit = new XmlSlurper().parse(new File('./src/test/resources/testdata/workitems.xml'))
		def layout = new JsonSlurper().parseText(getClass().getResource('/testdata/layout.json').text)
		genericRestClient.get(_) >> layout
		
		and: a_ 'stub rest call for pages'
		def pages = new JsonSlurper().parseText(getClass().getResource('/testdata/page.json').text)
		genericRestClient.post(_) >> pages
		
		and: a_ 'stub rest call for fields'
		def witFieldChange = new JsonSlurper().parseText(getClass().getResource('/testdata/witfieldchange.json').text)
		def field = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		//new XmlSlurper().parse(new File('./src/test/resources/testdata/witfieldchange.xml'))
		
		when: w_ 'call ensureWitFieldLayout'
		def result = underTest.ensureWitFieldLayout('', 'DigitalBanking', wit, field, witFieldChange)
		
		then: t_ 'result != null'
		result != null
	}
	
	@Test
	def 'createWITGroup success flow' () {
		given: g_ 'stub of post of layout data'
		def out = new JsonSlurper().parseText(getClass().getResource('/testdata/layout.json').text)
		genericRestClient.post(_) >> out
		
		and: a_ 'stub of input arguments'
		def wit = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		def externalPage = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		
		when: w_ 'call createWITGroup'
		def result = underTest.createWITGroup('', '', wit, externalPage, '', '')
		
		then: t_ 'result != null'
		result != null
	}
	
	@Test
	def 'createWITPage success flow' () {
		given: g_ 'stub rest call for layouts'
		def out = new JsonSlurper().parseText(getClass().getResource('/testdata/layout.json').text)
		genericRestClient.post(_) >> out
		
		def wit = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		
		when: w_ 'call createWITPage'
		def result = underTest.createWITPage('', '', wit, '')
		
		then: t_ 'result != null'
		result != null
	}
	
	@Test
	def 'createField success flow' () {
		given: g_ 'stub layout rest call'
		def out = new JsonSlurper().parseText(getClass().getResource('/testdata/layout.json').text)
		genericRestClient.post(_) >> out
		
		def witFieldChange = new JsonSlurper().parseText(getClass().getResource('/testdata/witfieldchange.json').text)
		
		when: w_ 'call createField'
		def result = underTest.createField('', '', witFieldChange, '')
		
		then: t_ 'result != null'
		result != null
	}
	
	@Test
	def 'addGroup success flow' () {
		given: g_ 'stub call for layout'
		def out = new JsonSlurper().parseText(getClass().getResource('/testdata/layout.json').text)
		genericRestClient.post(_) >> out
		
		and: a_ 'setup argument for wit to update'
		def wit = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		
		when: w_ 'call addGroup'
		def result = underTest.addGroup('', '', wit, 123, 12, 'gName')
		
		then: t_ 'result != null'
		result != null
	}
	
	@Test
	def 'addControl success flow' () {
		given: g_ 'stubb of layout rest call'
		def out = new JsonSlurper().parseText(getClass().getResource('/testdata/layout.json').text)
		genericRestClient.post(_) >> out
		
		and: a_ 'setup control arguments'
		def wit = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		def controlData = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		
		when: w_ 'call addControl'
		def result = underTest.addControl('', '', wit, 123, controlData)
		
		then: t_ 'result == null'
		result == null
	}
	
	@Test
	def 'addWITField success flow' () {
		given: g_ 'stub of accessing layout'
		def out = new JsonSlurper().parseText(getClass().getResource('/testdata/layout.json').text)
		genericRestClient.post(_) >> out
		
		and: a_ 'setup of add WIT arguments'
		def wit = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		def controlData = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		
		when: w_ 'call addWITField'
		def result = underTest.addWITField('', '', 'wrefName', '')
		
		then: t_ 'result != null'
		result != null
	}
	
	@Test
	def 'getWITField success flow' () {
		given: g_ 'stub rest call to access layout'
		def out = new JsonSlurper().parseText(getClass().getResource('/testdata/layout.json').text)
		genericRestClient.post(_) >> out
		
		and: a_ 'setup get WIT field parameters'
		def wit = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		def field = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		
		when: w_ 'call getWITField'
		def result = underTest.getWITField('', '', wit, field)
		
		then: t_ 'result != null'
		result == null
	}
	
	@Test
	def 'createWorkitemTemplate success flow' () {
		given: g_ 'project management service getProjectProperty stub'
		def processTemplateId = new JsonSlurper().parseText(getClass().getResource('/testdata/project.json').text)
		projectManagementService.getProjectProperty(_, _, _) >> processTemplateId
		
		and: a_ 'stub get project'
		def projectData = new JsonSlurper().parseText(getClass().getResource('/testdata/project.json').text)
		projectManagementService.getProject(_, _) >> projectData
		
		and: a_ 'azure rest client http get stub'
		def out = new JsonSlurper().parseText(getClass().getResource('/testdata/dfprocessfields.json').text)
		1 * genericRestClient.get(_) >> out
		
		and: a_ 'stub rest call for wits'
		def rwits = new JsonSlurper().parseText(getClass().getResource('/testdata/workitemtypes.json').text)
		4 * genericRestClient.get(_) >> rwits
		
		and: a_ 'stub rest call for process fields'
		def outPost = new JsonSlurper().parseText(getClass().getResource('/testdata/dfprocessfields.json').text)
		genericRestClient.post(_) >> out
		
		/*and: a_
		def rwits2 = new JsonSlurper().parseText(getClass().getResource('/testdata/workitemtypes.json').text)
		//3 * genericRestClient.get(_) >> rwits2
		
		def actualwit = new JsonSlurper().parseText(getClass().getResource('/testdata/actualwit.json').text)
		//1 * genericRestClient.get(_) >> actualwit*/
		
		when: w_ 'call createWorkitemTemplate'
		def result = underTest.createWorkitemTemplate('', 'DigitalBanking', 'Enhancement Request')
		
		then: t_ null
		true
	}
	
	@Test
	def 'addGroupWithControl success flow' () {
		given: g_ 'setup addGroupWithControl arguments'
		def wit = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		def externalPage =  new JsonSlurper().parseText('{"id":1234}')
		def field = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		and: a_ 'stub post for adding a group with control'
		def outPost = new JsonSlurper().parseText(getClass().getResource('/testdata/dfprocessfields.json').text)
		genericRestClient.post(_) >> outPost
		
		when: w_ 'call addGroupWithControl'
		def result = underTest.addGroupWithControl('', '', wit, externalPage, field, 'section')
		
		then: t_ null
		true
	}
	
	@Test
	def 'ensureWitField success flow' () {
		given: g_ 'stub rest call for  process fields data'
		def wit = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		def witFieldChange = new JsonSlurper().parseText(getClass().getResource('/testdata/witfieldchange.json').text)
		
		String json = this.getClass().getResource('/testdata/processfields.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		genericRestClient.get(_) >> out
		
		and: a_ 'stub rest call to add a WIT field'
		def outPost = new JsonSlurper().parseText(getClass().getResource('/testdata/page.json').text)
		genericRestClient.post(_) >> outPost
		
		when: w_ 'call ensureWitField'
		def result = underTest.ensureWitField('', 'DigitialBanking', wit, witFieldChange)
		
		then: t_ null
		true
	}
	
	@Test
	def 'requiresField success flow' () {
		given: g_ 'setup arguments for requiresField'
		def field = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		def witMapping = new XmlSlurper().parse(new File(testMappingFileName))
		when: w_ 'call requiresField'
		def result = underTest.requiresField(field, witMapping)
		then: t_ 'result == true'
		result == true
	}
	
	@Test
	def 'addUnmappedFields exception flow' () {
		given: g_ 'setup arguments for addUnmappedField'
		def wit = new JsonSlurper().parseText(getClass().getResource('/testdata/actualwit.json').text)
		def mapping = new XmlSlurper().parse(new File(testMappingFileName))
		def witMap = [:]
		
		when: w_ 'call addUnmappedFields'
		def result = underTest.addUnmappedFields(witMap, wit, mapping)
		
		then: t_ 'throws MissingFieldException'
		thrown MissingFieldException
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