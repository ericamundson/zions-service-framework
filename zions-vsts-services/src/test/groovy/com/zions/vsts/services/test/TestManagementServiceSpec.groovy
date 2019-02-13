package com.zions.vsts.services.test

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

import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.DataGenerationService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient
import com.zions.vsts.services.work.WorkManagementService
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
	WorkManagementService workManagementService;

	@Autowired
	ICacheManagementService cacheManagementService
	
	@Autowired
	TestManagementService underTest
	
	@Autowired
	DataGenerationService dataGenerationService

	def 'ensureResultAttachments success sending attachment to ADO' () {
		given: 'Stub call to cacheManagementService.getFromCache'
		def adoTestcase = dataGenerationService.generate('/testdata/wiData.json')
		1 * cacheManagementService.getFromCache(_, _) >> adoTestcase		

		
		and: 'Stub call to genericRestClient.get that gets attachments inside of hasAttachment (no attachment found)'
		def attachments = dataGenerationService.generate('/testdata/attachments.json')
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
		def rqmTestCase = dataGenerationService.generate('/testdata/testcase49881.xml')
		
		and: 'Data for result map'
		def results = dataGenerationService.generate('/testdata/resultsMap.json')
		def resultMap = [:]
		int count = 0
		results.'value'.each { result ->
			resultMap["${result.testCase.id}"] = result
			if (count == 0) {
				resultMap["${adoTestcase.id}"] = result
			}
			count++
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
		def adoTestcase = dataGenerationService.generate('/testdata/wiData.json')
		1 * cacheManagementService.getFromCache(_, _) >> adoTestcase		

		
		and: 'Stub call to genericRestClient.get that gets attachments inside of hasAttachment (no attachment found)'
		def attachments = dataGenerationService.generate('/testdata/attachments.json')
		1 * genericRestClient.get(_) >> attachments
		

		and: 'setup file to attach'
		File file = new File('textAsFileAttachment.txt')
		def os = file.newDataOutputStream()
		os << 'Test text'
		os.close()
		def rFiles = [[file:file, comment: 'out test']]
		
		and: 'Data for RQM test case'
		def rqmTestCase = dataGenerationService.generate('/testdata/testcase49881.xml')
		
		and: 'Data for result map'
		def results = dataGenerationService.generate('/testdata/resultsMap.json')
		def resultMap = [:]
		int count = 0
		results.'value'.each { result ->
			resultMap["${result.testCase.id}"] = result
			if (count == 0) {
				resultMap["${adoTestcase.id}"] = result
			}
			count++
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
		def change = dataGenerationService.generate('/testdata/changepost.json')
		
		and: 'ensure stub of post call'
		def plan = dataGenerationService.generate('/testdata/TestPlan.json')
		1 * genericRestClient.post(_) >> plan
		
		and: 'ensure stub of get work item.'
		def wi = dataGenerationService.generate('/testdata/wiDataT.json')
		1 * workManagementService.getWorkItem(_,_,_) >> wi

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
		def change = dataGenerationService.generate('/testdata/changepatch.json')
		
		and: 'ensure stub of post call'
		def plan = dataGenerationService.generate('/testdata/TestPlan.json')
		1 * genericRestClient.patch(_) >> plan
		
		and: 'ensure stub of get work item.'
		def wi = dataGenerationService.generate('/testdata/wiDataT.json')
		1 * workManagementService.getWorkItem(_,_,_) >> wi
		
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
		def change = dataGenerationService.generate('/testdata/resultpost.json')
		
		and: 'ensure stub of post call'
		def result = dataGenerationService.generate('/testdata/result.json')
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
		def change = dataGenerationService.generate('/testdata/resultpatch.json')
		
		and: 'ensure stub of post call'
		def result = dataGenerationService.generate('/testdata/result.json')
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
		def parentData =  dataGenerationService.generate('/testdata/TestPlan.json')
		def parent =  dataGenerationService.generate('/testdata/testplanT.xml')
		
		and: 'setup child test case data'
		def children = []
		for (int i = 0; i < 9; i++) {
			def child = dataGenerationService.generate('/testdata/testcaseT.xml')
			children.add(child)
		}
		
		and: 'stub parent cache request'
		1 * cacheManagementService.getFromCache(_,_) >> parentData
		
		and: 'stub child cache calls'
		for (int i = 0; i < 9; i++) {
			def tc1 =  dataGenerationService.generate('/testdata/testcaseT.json')
			1 * cacheManagementService.getFromCache(_,_) >> tc1
		}
		
		and: 'stub a ado call to associate a test case to a plan'
		2 * genericRestClient.post(_) >> [:]
		
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
	
	def 'setParent with suite and child test case'() {
		given: 'Setup map data'
		def map = getMappingData()
		
		and: 'setup parent data'
		def parentData =  dataGenerationService.generate('/testdata/testsuiteT.json')
		def parent =  dataGenerationService.generate('/testdata/testsuiteT.xml')
		
		and: 'setup child test case data'
		def children = []
		for (int i = 0; i < 9; i++) {
			def child = dataGenerationService.generate('/testdata/testcaseT.xml')
			children.add(child)
		}
		
		and: 'stub parent cache request'
		1 * cacheManagementService.getFromCache(_,_) >> parentData
		
		and: 'stub child cache calls'
		for (int i = 0; i < 9; i++) {
			def tc1 =  dataGenerationService.generate('/testdata/testcaseT.json')
			1 * cacheManagementService.getFromCache(_,_) >> tc1
		}
		
		and: 'stub a ado call to associate a test case to a plan'
		2 * genericRestClient.post(_) >> [:]
		
		when: 'call method under test setParent'
		boolean success = true
		try {
			def result = underTest.setParent(parent, children, map)
		} catch (e) {
			e.printStackTrace()
			success = false
		}
		
		then:
		success
	}

	def 'getTestRuns normal flow'() {
		given: 'Stub ado call to get projects'
		1 * projectManagmentService.getProject(_,_) >> dataGenerationService.generate('/testdata/project.json')
		
		and: 'stub ado call to get runs'
		1 * genericRestClient.get(_) >> [:]
		
		when: 'call method under test getTestRuns'
		def result = underTest.getTestRuns('')
		
		then:
		true
	}
	
	def 'cleanupTestItems normal flow'() {
		given: 'stub to query for team area test work items'
		1 * genericRestClient.getTfsUrl() >> ''
		1 * genericRestClient.post(_) >> dataGenerationService.generate('/testdata/wiqlResult.json')
		
		and: 'stub calls to get work item and delete them'
		for (int i = 0; i < 9; i++) {
			1 * genericRestClient.get(_) >> dataGenerationService.generate('/testdata/wiDataT.json')
			1 * genericRestClient.getTfsUrl() >> ''
			1 * genericRestClient.delete(_) >> [:]
		}
		
		when: 'calling method under test cleanupTestItems'
		boolean success = true
		try {
			def result = underTest.cleanupTestItems('', '', '')
		} catch (e) {
			success = false
		}
		
		then: 
		success
	}
	
	def 'ensureTestRun null result from cache'() {
		given: 'stub cache access the runData'
		1 * cacheManagementService.getFromCache(_,_) >> null
		
		and: 'stub get plan from cache'
		1 * cacheManagementService.getFromCache(_,_) >> dataGenerationService.generate('/testdata/TestPlan.json')
		
		and: 'stub post to create run data'
		1 * genericRestClient.get(_) >> dataGenerationService.generate('/testdata/points.json')
		1 * genericRestClient.post(_) >> dataGenerationService.generate('/testdata/runData.json')
		
		and: 'stub saving to cache'
		1 * cacheManagementService.saveToCache(_, _, _) >> [:]
		
		and: 'stub getting map'
		1 * genericRestClient.get(_) >> dataGenerationService.generate('/testdata/resultsMap.json')
		
		when: 'call method under test ensureTestRun'
		def planData = dataGenerationService.generate('/testdata/testplanT.xml')
		boolean success = true
		try {
			def result = underTest.ensureTestRun('', '', planData)
		} catch (e) {
			success = false
		}
		
		then:
		success
	}
	
	def getMappingData() {
		def mappingDataInfo = []
		def xmlMappingData = dataGenerationService.generate('/testdata/CoreRQMMapping.xml')
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
@ComponentScan(["com.zions.common.services.test", "com.zions.vsts.services.test", "com.zions.common.services.restart"])
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
	WorkManagementService workManagementService() {
		return mockFactory.Mock(WorkManagementService)
	}

	@Bean
	TestManagementService underTest() {
		return new TestManagementService()
	}
	
	@Bean
	DataGenerationService dataGenerationService() {
		return new DataGenerationService()
	}
	
}