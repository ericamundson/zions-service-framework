package com.zions.vsts.services.setowner

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
		resp == 'Not a target work item type'
	}
	
	def "State is not being Closed"() {
		given: "A mock ADO event payload where state is not Closed"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataStateNotClosed.json').text)

		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No updates should be made"
		resp == 'Work item not being closed'
	}
	
	def "No fields were changed"() {
		given: "A mock ADO event payload where state is not Closed"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataNoFieldsChanged.json').text)

		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No updates should be made"
		resp == 'Work item not being closed'
	}

	def "Work Item Aready Assigned"() {
		given: "A mock ADO event payload where Assigned To is already set"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataAlreadyAssigned.json').text)

		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No updates should be made"
		resp == 'Work item already assigned'
	}
	
	def "Work Item Has No Parent"() {
		given: "A mock ADO event payload where work item has no parent"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataNoParent-SvcAccount.json').text)

		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No updates should be made"
		resp == 'No parent'
	}

	def "Svc Account and Parent is Unassigned"() {
		given: "A mock ADO event payload for WI having unassigned parent"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataParentNotAssigned-SvcAccount.json').text)
		
		and: "stub workManagementService.getWorkItem()"
		workManagementService.getWorkItem(_,_,_) >> {
			return new JsonSlurper().parseText(this.getClass().getResource('/testdata/unassignedParentData.json').text)
		}

		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No updates should be made"
		resp == 'Parent is unassigned'
	}

	def "Error Retrieving Parent Work Item"() {
		given: "A mock ADO event payload exists that meets all criteria for update"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataSuccessfulAssignment-SvcAccount.json').text)

		and: "stub workManagementService.getWorkItem()"
		workManagementService.getWorkItem(_,_,_) >> null // could not retrieve parent

		when: "calling method under test processADOData()"
		def resp = underTest.processADOData(adoMap)
		
		then: "Error Retrieving Parent"
		resp == 'Error Retrieving Parent'
	}

	def "Successful Assignment to Modifier"() {
		given: "A mock ADO event payload exists that meets all criteria for update"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataSuccessfulAssignment.json').text)

		and: "stub workManagementService.updateItem()"
		workManagementService.updateWorkItem(_,_,_,_,_) >> { args ->
			String data = "${args[3]}"
			assert(data.toString() == '[[op:test, path:/rev, value:2], [op:add, path:/fields/System.AssignedTo, value:robert.huet@zionsbancorp.com]]')
			return true
		}

		when: "calling method under test processADOData()"
		def resp = underTest.processADOData(adoMap)
		
		then: "Update should be made"
		resp == 'Work item successfully assigned to modifier'
	}
	def "Unsuccessful Assignment to Modifier"() {
		given: "A mock ADO event payload exists that meets all criteria for update"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataSuccessfulAssignment.json').text)

		and: "stub workManagementService.updateItem()"
		workManagementService.updateWorkItem(_,_,_,_,_) >> {
			underTest.retryFailed = true
			return null // patch returned an error
		}

		when: "calling method under test processADOData()"
		def resp = underTest.processADOData(adoMap)
		
		then: "Update should not be made"
		resp == 'Error assigning to modifier'
	}

	def "Successful Assignment to Parent Owner"() {
		given: "A mock ADO event payload exists that meets all criteria for update"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataSuccessfulAssignment-SvcAccount.json').text)

		and: "stub workManagementService.getWorkItem()"
		workManagementService.getWorkItem(_,_,_) >> {
			return new JsonSlurper().parseText(this.getClass().getResource('/testdata/parentData.json').text)
		}
		
		and: "stub workManagementService.updateItem()"
		workManagementService.updateWorkItem(_,_,_,_,_) >> { args ->
			String data = "${args[3]}"
			assert(data.toString() == '[[op:test, path:/rev, value:2], [op:add, path:/fields/System.AssignedTo, value:robert.huet@zionsbancorp.com]]')
			return true
		}

		when: "calling method under test processADOData()"
		def resp = underTest.processADOData(adoMap)
		
		then: "Update should be made"
		resp == 'Work item successfully assigned to parent'
	}
	
	def "Unsuccessful Assignment to Parent Owner"() {
		given: "A mock ADO event payload exists that meets all criteria for update"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataSuccessfulAssignment-SvcAccount.json').text)

		and: "stub workManagementService.getWorkItem()"
		workManagementService.getWorkItem(_,_,_) >> {
			return new JsonSlurper().parseText(this.getClass().getResource('/testdata/parentData.json').text)
		}
		
		and: "stub workManagementService.updateItem()"
		workManagementService.updateWorkItem(_,_,_,_,_) >> {
			underTest.retryFailed = true
			return null // patch returned an error
		}

		when: "calling method under test processADOData()"
		def resp = underTest.processADOData(adoMap)
		
		then: "Update should not be made"
		resp == 'Error assigning to parent owner'
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