package com.zions.vsts.services.test

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient
import com.zions.vsts.services.work.WorkManagementServiceConfig
import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[TestManagementServiceSpecConfig])
class TestManagementServiceSpec extends Specification {
	
	@Autowired(required=true)
	private IGenericRestClient genericRestClient;
	
	@Autowired(required=true)
	private ProjectManagementService projectManagmentService;

	@Autowired
	ICacheManagementService cacheManagementService
	
	@Autowired
	TestManagementService underTest

	def 'ensureResultAttachments success sending attachment to ADO' () {
		given: 'Stub call to cacheManagementService.getFromCache'
		def adoTestcase = new JsonSlurper().parseText(this.getClass().getResource('/testdata/wiData.json').text)
		1 * cacheManagementService.getFromCache(_, _) >> adoTestcase		

		
		and: 'Stub call to genericRestClient.get that gets attachments inside of hasAttachment (no attachment found)'
		def attachments = new JsonSlurper().parseText(this.getClass().getResource('/testdata/attachments.json').text)
		1 * genericRestClient.get(_) >> attachments
		
		and: 'Stub call to genericRestClient.post that send attachment to ADO'
		1 * genericRestClient.post(_) >> [id: 4, url:'https://dev.azure.com/fabrikam/Fabrikam/_apis/test/Runs/49/Results/100000/Attachments/4']
		and: 'setup file to attach'
		File file = new File('test.txt')
		def os = file.newDataOutputStream()
		os << 'Test text'
		os.close()
		def rFiles = [[file:file, comment: 'out test']]
		
		and: 'Data for RQM test case'
		def rqmTestCase = new XmlSlurper().parseText(this.getClass().getResource('/testdata/testcase49881.xml').text)
		
		and: 'Data for result map'
		def results = new JsonSlurper().parseText(this.getClass().getResource('/testdata/resultsMap.json').text)
		def resultMap = [:]
		results.'value'.each { result ->
			resultMap["${result.testCase.id}"] = result
		}
		when: 'make call to method under test ensureResultAttachments'
		boolean success = true
		try {
			def result = underTest.ensureResultAttachments('', '', rFiles, rqmTestCase, resultMap)
		} catch (e) {
			success = false
		}
		
		then: 'validate result of attachment Request'
		success
	}
	
	def 'ensureResultAttachments attachment already exists in ADO' () {
		given: 'Stub call to cacheManagementService.getFromCache'
		def adoTestcase = new JsonSlurper().parseText(this.getClass().getResource('/testdata/wiData.json').text)
		1 * cacheManagementService.getFromCache(_, _) >> adoTestcase		

		
		and: 'Stub call to genericRestClient.get that gets attachments inside of hasAttachment (no attachment found)'
		def attachments = new JsonSlurper().parseText(this.getClass().getResource('/testdata/attachments.json').text)
		1 * genericRestClient.get(_) >> attachments
		

		and: 'setup file to attach'
		File file = new File('textAsFileAttachment.txt')
		def os = file.newDataOutputStream()
		os << 'Test text'
		os.close()
		def rFiles = [[file:file, comment: 'out test']]
		
		and: 'Data for RQM test case'
		def rqmTestCase = new XmlSlurper().parseText(this.getClass().getResource('/testdata/testcase49881.xml').text)
		
		and: 'Data for result map'
		def results = new JsonSlurper().parseText(this.getClass().getResource('/testdata/resultsMap.json').text)
		def resultMap = [:]
		results.'value'.each { result ->
			resultMap["${result.testCase.id}"] = result
		}
		
		when: 'make call to method under test ensureResultAttachments'
		boolean success = true
		try {
			def result = underTest.ensureResultAttachments('', '', rFiles, rqmTestCase, resultMap)
		} catch (e) {
			success = false
		}
		
		then: 'validate result of attachment Request'
		success
	}
	
	def 'sendPlanChanges with new plan item'() {
		given:'Setup change data'
		def change = new JsonSlurper().parseText(this.getClass().getResource('/testdata/changepost.json').text)
		
		and: 'ensure stub of post call'
		def plan = new JsonSlurper().parseText(this.getClass().getResource('/testdata/Test Plan.json').text)
		1 * genericRestClient.post(_) >> plan
		
		when: 'Run method under test sendPlanChanges'
		boolean success = true
		try {
			underTest.sendPlanChanges('', '', change,'aIdk')
		} catch (e) {
			success = false
		}
		then:
		success
	}
	
	def 'sendPlanChanges with update plan item'() {
		given:'Setup change data'
		def change = new JsonSlurper().parseText(this.getClass().getResource('/testdata/changepatch.json').text)
		
		and: 'ensure stub of post call'
		def plan = new JsonSlurper().parseText(this.getClass().getResource('/testdata/Test Plan.json').text)
		1 * genericRestClient.patch(_) >> plan
		
		when: 'Run method under test sendPlanChanges'
		boolean success = true
		try {
			underTest.sendPlanChanges('', '', change,'aIdk')
		} catch (e) {
			success = false
		}
		then:
		success
	}

	def 'sendResultChanges with new result'() {
		given:'Setup change data'
		def change = new JsonSlurper().parseText(this.getClass().getResource('/testdata/resultpost.json').text)
		
		and: 'ensure stub of post call'
		def result = new JsonSlurper().parseText(this.getClass().getResource('/testdata/result.json').text)
		1 * genericRestClient.post(_) >> result
		
		when: 'Run method under test sendResultChanges'
		boolean success = true
		try {
			underTest.sendResultChanges('', '', change,'aIdk')
		} catch (e) {
			success = false
		}
		then:
		success
	}
	def 'sendResultChanges with update result'() {
		given:'Setup change data'
		def change = new JsonSlurper().parseText(this.getClass().getResource('/testdata/resultpatch.json').text)
		
		and: 'ensure stub of post call'
		def result = new JsonSlurper().parseText(this.getClass().getResource('/testdata/result.json').text)
		1 * genericRestClient.patch(_) >> result
		
		when: 'Run method under test sendResultChanges'
		boolean success = true
		try {
			underTest.sendResultChanges('', '', change,'aIdk')
		} catch (e) {
			success = false
		}
		then:
		success
	}
	
	def 'setParent with plan and child test case'() {
		given: 'Setup map data'
		def map = getMappingData()
		
		and: 'setup parent data'
		def parentData =  new JsonSlurper().parseText(this.getClass().getResource('/testdata/Test Plan.json').text)
		def parent =  new XmlSlurper().parseText(this.getClass().getResource('/testdata/testplan218.xml').text)
		
		and: 'setup child test case data'
		def children = []
		def child = new XmlSlurper().parseText(this.getClass().getResource('/testdata/testcase49881.xml').text)
		children.add(child)
		child = new XmlSlurper().parseText(this.getClass().getResource('/testdata/testcase49886.xml').text)
		children.add(child)
		
		and: 'stub parent cache request'
		1 * cacheManagementService.getFromCache(_,_) >> parentData
		
		and: 'stub child cache calls'
		def tc1 =  new JsonSlurper().parseText(this.getClass().getResource('/testdata/testcase1.json').text)
		1 * cacheManagementService.getFromCache(_,_) >> tc1
		def tc2 =  new JsonSlurper().parseText(this.getClass().getResource('/testdata/testcase2.json').text)
		1 * cacheManagementService.getFromCache(_,_) >> tc2

		
		and: 'stub a ado call to associate a test case to a plan'
		1 * genericRestClient.post(_) >> [:]
		
		when: 'call method under test setParent'
		boolean success = true
		try {
			def result = underTest.setParent(parent, children, map)
		} catch (e) {
			success = false
		}
		
		then:
		success
	}
	
	def getMappingData() {
		def mappingDataInfo = []
		def xmlMappingData = new XmlSlurper().parseText(this.getClass().getResource('/testdata/CoreRQMMapping.xml').text)
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
		//Test data
		return mappingDataInfo
	}

}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class TestManagementServiceSpecConfig {
	def mockFactory = new DetachedMockFactory()
	
	@Bean
	IGenericRestClient genericRestClient() {
		return mockFactory.Mock(GenericRestClient)
	}
	
	@Bean
	ProjectManagementService projectManagmentService() {
		return mockFactory.Mock(ProjectManagementService)
	}
	
	@Bean
	ICacheManagementService cacheManagementService() {
		return mockFactory.Mock(ICacheManagementService)
	}
	
	@Bean
	TestManagementService underTest() {
		return new TestManagementService()
	}
	
}