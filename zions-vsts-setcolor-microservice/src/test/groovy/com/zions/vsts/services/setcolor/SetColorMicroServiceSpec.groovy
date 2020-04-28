package com.zions.vsts.services.setcolor

import static org.junit.Assert.*

import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.SpockLabeler
import com.zions.vsts.services.tfs.rest.GenericRestClient
import com.zions.vsts.services.work.WorkManagementService

import groovyx.net.http.ContentType
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration
import groovy.json.JsonSlurper

import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[SetOwnerMicroserviceTestConfig])
class SetOwnerMicroServiceSpec extends Specification {
	@Autowired
	SetOwnerMicroService underTest;
	
	@Autowired
	WorkManagementService workManagementService;
	
	def "Not a Bug work item type"() {
		given: "A mock ADO event payload exists for wrong work item type"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataWrongType.json').text)

		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No updates should be made"
		resp == false
	}
	
	def "No change to Severity, Priority or Color"() {
		given: "A mock ADO event payload where state is not Closed"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataNoRelevantChange.json').text)

		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No updates should be made"
		resp == false
	}
	
	def "Severity and Priority are set, but Color is unassigned"() {
		given: "A mock ADO event payload where Assigned To is already set"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataMissingColor.json').text)

		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No updates should be made"
		resp == false
	}
	
	def "Severity and Priority are set, and Color is wrong"() {
		given: "A mock ADO event payload where work item has no parent"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataWrongColor.json').text)

		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No updates should be made"
		resp == false
	}

	def "Severity and Priority are set, and Color is correct"() {
		given: "A mock ADO event payload for WI having unassigned parent"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataCorrectColor.json').text)
		
		and: "stub workManagementService.getWorkItem()"
		workManagementService.getWorkItem(_,_,_) >> {
			return new JsonSlurper().parseText(this.getClass().getResource('/testdata/unassignedParentData.json').text)
		}

		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No updates should be made"
		resp == false
	}
	
	def "Either Severity or Priority is unassigned, but Color is set"() {
		given: "A mock ADO event payload exists that meets all criteria for update"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataPrematureColor.json').text)

		and: "stub workManagementService.getWorkItem()"
		workManagementService.getWorkItem(_,_,_) >> {
			return new JsonSlurper().parseText(this.getClass().getResource('/testdata/parentData.json').text)
		}
		
		and: "stub workManagementService.updateItem()"
		workManagementService.updateWorkItem(_,_,_,_) >> { args ->
			String data = "${args[3]}"
			assert(data.toString() == '[[op:test, path:/rev, value:2], [op:add, path:/fields/System.AssignedTo, value:robert.huet@zionsbancorp.com]]')
		}

		when: "calling method under test processADOData()"
		def resp = underTest.processADOData(adoMap)
		
		then: "Update should be made"
		resp == true
	}
	
}

@TestConfiguration
@Profile("test")
@ComponentScan(["com.zions.vsts.services.work.WorkManagementService","com.zions.common.services.rest"])
@PropertySource("classpath:test.properties")
class SetOwnerMicroserviceTestConfig {
	def mockFactory = new DetachedMockFactory()
	
	@Value('${tfs.types}') 
	String wiTypes
	
	@Bean
	SetOwnerMicroService underTest() {
		return new SetOwnerMicroService()
	}
	@Bean
	WorkManagementService workManagementService() {
		return mockFactory.Stub(WorkManagementService)
	}
	@Bean
	IGenericRestClient genericRestClient() {
		return mockFactory.Stub(GenericRestClient)
	}
}