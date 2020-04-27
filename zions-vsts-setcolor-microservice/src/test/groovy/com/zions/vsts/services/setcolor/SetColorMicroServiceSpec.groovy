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
	
	def "Not a configured target work item type"() {
		given: "A mock ADO event payload exists for wrong work item type"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataWrongType.json').text)

		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No updates should be made"
		resp == false
	}
	
	def "State is not Closed"() {
		given: "A mock ADO event payload where state is not Closed"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataStateNotClosed.json').text)

		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No updates should be made"
		resp == false
	}
	
	def "Work Item Aready Assigned"() {
		given: "A mock ADO event payload where Assigned To is already set"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataAlreadyAssigned.json').text)

		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No updates should be made"
		resp == false
	}
	
	def "Work Item Has No Parent"() {
		given: "A mock ADO event payload where work item has no parent"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataNoParent.json').text)

		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No updates should be made"
		resp == false
	}

	def "Parent is Unassigned"() {
		given: "A mock ADO event payload for WI having unassigned parent"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataParentNotAssigned.json').text)
		
		and: "stub workManagementService.getWorkItem()"
		workManagementService.getWorkItem(_,_,_) >> {
			return new JsonSlurper().parseText(this.getClass().getResource('/testdata/unassignedParentData.json').text)
		}

		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No updates should be made"
		resp == false
	}
	
	def "Successful Assignment to Parent Owner"() {
		given: "A mock ADO event payload exists that meets all criteria for update"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataSuccessfulAssignment.json').text)

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