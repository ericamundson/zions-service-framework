package com.zions.vsts.services

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

@ContextConfiguration(classes=[ChangeParentMicroserviceTestConfig])
class ChangeParentMicroServiceSpec extends Specification {
	@Autowired
	ChangeParentMicroService underTest;
	
	@Autowired
	WorkManagementService workManagementService;
	
	
	//passing
	def "Not a valid work item type"() {
		given: "A mock ADO event payload exists for wrong work item type"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataInvalidParentType.json').text)

		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No updates should be made"
		resp == 'not a valid work item type'
}
	
	//passing
	def "Work item changes not applicable"() {
		given: "A mock ADO event payload where parent work item field is not populated"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataFieldNotPopulated.json').text)
		

		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No updates should be made"
		resp == 'No valid changes made'
		//resp == 'no update needed'
		
	}
	

	
	//not working
	def "Successful event for adding inherited values"() {
		given: "A mock ADO event payload exists that meets all criteria for update"
		//get current work item data from json file
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataAddParent.json').text)
			
		//Get parent data from parent json file
		and: "stub workManagementService.getWorkItem()parent data"
		workManagementService.getWorkItem(_,_,_) >> {
		
		return new JsonSlurper().parseText(this.getClass().getResource('/testdata/parentDataArray.json').text)
		}
		//update current work item with data from parent		
		workManagementService.updateWorkItem(_,_,_,_) >> {
			String data = "${args[3]}"
			
			  assert(data.toString() == '[[op:test, path:/rev, value:2], [op:add, path:/fields/Custom.OTLNumber, value:8005]]')
		}
		
		when: "calling method under test processADOData()"
		def resp = underTest.processADOData(adoMap)
		
		then: "Update should be made"
		resp == 'Update Succeeded'
	}
	
	def "valid event for clearing inherited values"() {
		given: "A mock ADO event payload where work item has no parent"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/updateCurrent.json').text)
		//override work item and get work item details and return json file with valid bug count values
		and: "stub workManagementService.getWorkItem()"
		workManagementService.getWorkItem(_,_,_) >> {
		
			return new JsonSlurper().parseText(this.getClass().getResource('/testdata/updateCurrent.json').text)
		}
		
		//override update work item with test data values
		and: "stub workManagementService.updateItem()"
		
			workManagementService.updateWorkItem(_,_,_,_) >> {
			String data = "${args[3]}"
			
			  assert(data.toString() == '[[op:test, path:/rev, value:8], [op:add, path:/fields/Custom.OTLNumber, value:]]')
		}
		
		when: "ADO sends notification for work item change who's type is in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "Update should be made"
		resp == 'Update Succeeded'
	}
	
}

@TestConfiguration
@Profile("test")
@ComponentScan(["com.zions.vsts.services.work.WorkManagementService","com.zions.common.services.rest"])
@PropertySource("classpath:test.properties")
class ChangeParentMicroserviceTestConfig {
	def mockFactory = new DetachedMockFactory()
	
	@Value('${tfs.types}')
	String wiTypes
	
	@Bean
	ChangeParentMicroService underTest() {
		return new ChangeParentMicroService()
	}
	@Bean
	WorkManagementService workManagementService() {
		//return mockFactory.Mock(WorkManagementService)
		return mockFactory.Stub(WorkManagementService)
	}
	@Bean
	IGenericRestClient genericRestClient() {
		//return new GenericRestClient('http://localhost:8080/ws', '', '')
		return mockFactory.Stub(GenericRestClient)
	}
}