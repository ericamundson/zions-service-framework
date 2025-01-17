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

	def 'getWorkitems success flow.'() {
		given: 'project management service getProjecProperty stub'
		1 * projectManagementService.getProjectProperty(_, _ , _) >> "aTemplateId"
		
		and: 'azure rest client http get stub'
		def rwits = new JsonSlurper().parseText(getClass().getResource('/testdata/workitemtypes.json').text)
		1 * genericRestClient.get(_) >> rwits
		
		when: 'call method (getWorkitems) under test.'
		def wits = underTest.getWorkItemTypes('', 'project')
		
		then: 'wits.size() > 0'
		wits.size() > 0
	}
	
	def 'getWorkItemType success flow.'() {
		given: 'project management service getProjecProperty stub'
		1 * projectManagementService.getProjectProperty(_, _ , _) >> "aTemplateId"
		
		and: 'azure rest client http get stub'
		def out = new JsonSlurper().parseText(getClass().getResource('/testdata/processDTSTest.json').text)
		1 * genericRestClient.get(_) >> out
		
		and: 'setup parameters'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when: 'call method (getWorkItemType) under test.'
		def result = underTest.getWorkItemType('', 'DigitalBanking',  'DTSTest.EnhancementRequest','none','System.ProcessTemplateType')
		
		then: 'result.name == Enhancement Request'
		"${result.name}" == "Enhancement Request"
	}
	
	/*def 'getWorkitemTemplateXML success flow.'() {
		
		when: 'call method (getWorkitemTemplateXML) under test.'
		def result = underTest.getWorkitemTemplateXML('',  '',  '')
		
		then: 'No exception'
		result == null
	}
	*/
	def 'getField success flow.'() {
		given: 'stub of get repos rest call'
		String json = this.getClass().getResource('/testdata/repos.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		String url = "https://dev.azure.com/eto-dev/DigitalBanking/_apis/git/repositories/"
		
		when: 'call method (getField) under test.'
		def result = underTest.getField(url)
		
		then: 'result.count == 4'
		"${result.count}" == "4"
	}
	
	def 'getFields success flow.'() {
		given: 'stub get process fields rest call.'
		String json = this.getClass().getResource('/testdata/processfields.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when: 'call method (getFields) under test.'
		def result = underTest.getFields('', project)
		
		then: 'result.count == 220'
		"${result.count}" == "220"
	}
	
	def 'queryForField success flow.'() {
		given: 'stub get process fields rest call'
		String json = this.getClass().getResource('/testdata/processfields.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		genericRestClient.get(_) >> out  // May or may not be called depending on caching
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when: 'call method (queryForField) under test.'
		def result = underTest.queryForField('', project, 'System.State')
		
		then: 'No exception'
		result != null
	}
	
	def 'getField success flow with three params'() {
		given: 'project management service getProject stub'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		1 * projectManagementService.getProject(_, _ ) >> project
		
		and: 'Stub rest call'
		String json = this.getClass().getResource('/testdata/Microsoft.VSTS.Common.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		when: 'call method (getField) under test.'
		def result = underTest.getField('', 'DigitalBanking', '')
		
		then: "Result name is 'Acceptance Criteria'"
		"${result.name}" == "Acceptance Criteria"
	}
	
	def 'updateWorkitemTemplate success flow with three params'() {
		given: 'stub get some bugs rest call'
		String json = this.getClass().getResource('/testdata/processbug.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.put(_) >> out
		
		when: 'call method (updateWorkitemTemplate) under test.'
		def result = underTest.updateWorkitemTemplate('', 'DigitalBanking', '', '')
		
		then: 'result.name == Bug'
		"${result.name}" == "Bug"
	}
		
	def 'createPickList success flow'() {
		given: 'stub of rest call for process lists'
		String json = this.getClass().getResource('/testdata/processlists.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.post(_) >> out
		
		and: 'stub of rest call of pick lists'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def witFieldChange= [:]
		
		when: 'call method (createPickList)'
		def result = underTest.createPickList('', project, witFieldChange)
		
		then: "result.count == 15"
		"${result.count}" == "15"
	}
	
	def 'genColor success flow'() {
		
		when: 'call method (genColor) under test.'
		def result = underTest.genColor()
		
		then: 'result != null'
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
		
		then: 'No exception'
		//result != null
	}*/
	
	def 'ensureWITChanges success flow.'() {
		given: 'Stub data'
		def mapping = new XmlSlurper().parse(new File(testMappingFileName))
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def changes = []
		def change = [id: 1, displayName: 'abc', from: 'a', to: 'b']
		changes.add(change)
		when: 'call ensureWITChanges'
		def result = underTest.ensureWITChanges('', project, '')
		
		then: 'No exception'
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
	
	def 'getWIT success flow' () {
		given: 'stub rest call for work item type data'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def changes = new XmlSlurper().parse(new File('./src/test/resources/testdata/workitems.xml'))
		def rwits = new JsonSlurper().parseText(getClass().getResource('/testdata/workitemtypes.json').text)
		genericRestClient.get(_) >> rwits
		
		when: 'call getwit'
		def resultOne = underTest.getWIT('eto-dev', 'DigitalBanking', 'Enhancement Request')
		def resultTwo = underTest.getWIT('eto-dev', 'DigitalBanking', 'Something')
		
		then: 'result == null'
		resultOne == null
		resultTwo == null
	}
	
	
	def 'ensureWitFieldLayout success flow' () {
		given: 'stub rest call for layout'
		def wit = new JsonSlurper().parseText(getClass().getResource('/testdata/wit.json').text)
		
		and: 'stub rest call for pages'
		def pages = new JsonSlurper().parseText(getClass().getResource('/testdata/page.json').text)
		genericRestClient.post(_) >> pages
		
		and: 'stub rest call for fields'
		def witFieldChange = new JsonSlurper().parseText(getClass().getResource('/testdata/witfieldchange.json').text)
		def field = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		//new XmlSlurper().parse(new File('./src/test/resources/testdata/witfieldchange.xml'))
		
		when: 'call ensureWitFieldLayout'
		def result = underTest.ensureWitFieldLayout('', 'DigitalBanking', wit, field, witFieldChange)
		
		then: 'result != null'
		result != null
	}
	
	
	def 'createWITGroup success flow' () {
		given: 'stub of post of layout data'
		def out = new JsonSlurper().parseText(getClass().getResource('/testdata/layout.json').text)
		genericRestClient.post(_) >> out
		
		and: 'stub of input arguments'
		def wit = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		def externalPage = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		
		when: 'call createWITGroup'
		def result = underTest.createWITGroup('', '', wit, externalPage, '', '')
		
		then: 'result != null'
		result != null
	}
	
	
	def 'createWITPage success flow' () {
		given: 'stub rest call for layouts'
		def out = new JsonSlurper().parseText(getClass().getResource('/testdata/layout.json').text)
		genericRestClient.post(_) >> out
		
		def wit = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		
		when: 'call createWITPage'
		def result = underTest.createWITPage('', '', wit, '')
		
		then: 'result != null'
		result != null
	}
	
	
	def 'createField success flow' () {
		given: 'stub layout rest call'
		def out = new JsonSlurper().parseText(getClass().getResource('/testdata/layout.json').text)
		genericRestClient.post(_) >> out
		
		def witFieldChange = new JsonSlurper().parseText(getClass().getResource('/testdata/witfieldchange.json').text)
		
		when: 'call createField'
		def result = underTest.createField('', '', witFieldChange, '')
		
		then: 'result != null'
		result != null
	}
	
	
	def 'addGroup success flow' () {
		given: 'stub call for layout'
		def out = new JsonSlurper().parseText(getClass().getResource('/testdata/layout.json').text)
		genericRestClient.post(_) >> out
		
		and: 'setup argument for wit to update'
		def wit = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		
		when: 'call addGroup'
		def result = underTest.addGroup('', '', wit, 123, 12, 'gName')
		
		then: 'result != null'
		result != null
	}
	
	
	def 'addControl success flow' () {
		given: 'stubb of layout rest call'
		def out = new JsonSlurper().parseText(getClass().getResource('/testdata/layout.json').text)
		genericRestClient.post(_) >> out
		
		and: 'setup control arguments'
		def wit = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		def controlData = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		
		when: 'call addControl'
		def result = underTest.addControl('', '', wit, 123, controlData)
		
		then: 'result == null'
		result == null
	}
	
	
	def 'addWITField success flow' () {
		given: 'stub of accessing layout'
		def out = new JsonSlurper().parseText(getClass().getResource('/testdata/layout.json').text)
		genericRestClient.post(_) >> out
		
		and: 'setup of add WIT arguments'
		def wit = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		def controlData = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		
		when: 'call addWITField'
		def result = underTest.addWITField('', '', 'wrefName', '', '')
		
		then: 'result != null'
		result != null
	}
	
	
	def 'getWITField success flow' () {
		given: 'stub rest call to access layout'
		def out = new JsonSlurper().parseText(getClass().getResource('/testdata/layout.json').text)
		genericRestClient.post(_) >> out
		
		and: 'setup get WIT field parameters'
		def wit = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		def field = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		
		when: 'call getWITField'
		def result = underTest.getWITField('', '', wit, field)
		
		then: 'result != null'
		result == null
	}
	
	
	def 'createWorkitemTemplate success flow' () {
		given: 'project management service getProjectProperty stub'
		def processTemplateId = new JsonSlurper().parseText(getClass().getResource('/testdata/project.json').text)
		projectManagementService.getProjectProperty(_, _, _) >> processTemplateId
		
		and: 'stub get project'
		def projectData = new JsonSlurper().parseText(getClass().getResource('/testdata/project.json').text)
		projectManagementService.getProject(_, _) >> projectData
		
		and: 'azure rest client http get stub'
		def out = new JsonSlurper().parseText(getClass().getResource('/testdata/dfprocessfields.json').text)
		1 * genericRestClient.get(_) >> out
		
		and: 'stub rest call for wits'
		def rwits = new JsonSlurper().parseText(getClass().getResource('/testdata/workitemtypes.json').text)
		4 * genericRestClient.get(_) >> rwits
		
		and: 'stub rest call for process fields'
		def outPost = new JsonSlurper().parseText(getClass().getResource('/testdata/dfprocessfields.json').text)
		genericRestClient.post(_) >> out
		
		/*and:
		def rwits2 = new JsonSlurper().parseText(getClass().getResource('/testdata/workitemtypes.json').text)
		//3 * genericRestClient.get(_) >> rwits2
		
		def actualwit = new JsonSlurper().parseText(getClass().getResource('/testdata/actualwit.json').text)
		//1 * genericRestClient.get(_) >> actualwit*/
		
		when: 'call createWorkitemTemplate'
		def result = underTest.createWorkitemTemplate('', 'DigitalBanking', 'Enhancement Request')
		
		then: 'No exception'
		true
	}
	
	
	def 'addGroupWithControl success flow' () {
		given: 'setup addGroupWithControl arguments'
		def wit = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		def externalPage =  new JsonSlurper().parseText('{"id":1234}')
		def field = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		and: 'stub post for adding a group with control'
		def outPost = new JsonSlurper().parseText(getClass().getResource('/testdata/dfprocessfields.json').text)
		genericRestClient.post(_) >> outPost
		
		when: 'call addGroupWithControl'
		def result = underTest.addGroupWithControl('', '', wit, externalPage, field, 'section')
		
		then: 'No exception'
		true
	}
	
	
	def 'ensureWitField success flow' () {
		given: 'stub rest call for  process fields data'
		def wit = new JsonSlurper().parseText(getClass().getResource('/testdata/field.json').text)
		def witFieldChange = new JsonSlurper().parseText(getClass().getResource('/testdata/witfieldchange.json').text)
		
		String json = this.getClass().getResource('/testdata/processfields.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		genericRestClient.get(_) >> out
		
		and: 'stub rest call to add a WIT field'
		def outPost = new JsonSlurper().parseText(getClass().getResource('/testdata/page.json').text)
		genericRestClient.post(_) >> outPost
		
		when: 'call ensureWitField'
		def result = underTest.ensureWitField('', 'DigitialBanking', wit, witFieldChange)
		
		then: 'No exception'
		true
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