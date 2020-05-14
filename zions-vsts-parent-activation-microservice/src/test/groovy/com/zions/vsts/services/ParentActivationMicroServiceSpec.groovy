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

@ContextConfiguration(classes=[ParentActivationMicroserviceTestConfig])
class ParentActivationMicroServiceSpec extends Specification {
	@Autowired
	ParentActivationMicroService underTest;
	
	@Autowired
	WorkManagementService workManagementService;
	
	def "Not a valid child state for parent activation"() {
		given: "A mock ADO event payload exists for invalid child state"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataWrongState.json').text)

		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No Updates should be made"
		resp == 'not a valid child state'
	}
	
	def "Parent work item is already active"() {
		given: "A mock ADO event payload where parent work item state is already Active"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataAlreadyActive.json').text)
		
		and: "stub workManagementService.getWorkItem()"
		workManagementService.getWorkItem(_,_,_) >> {
		
		return new JsonSlurper().parseText(this.getClass().getResource('/testdata/parentDataAlreadyActive.json').text)
		}

		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No updates should be made"
		resp == 'parent is already active'
	}
	

	def "Work Item Has No Parent"() {
		given: "A mock ADO event payload where work item has no parent"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataNoParent.json').text)

		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No updates should be made"
		resp == 'parent not assigned'
	}

	def "Successful Activation of Parent Work Item"() {
		given: "A mock ADO event payload exists that meets all criteria for update"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataSuccessfulActivation.json').text)

		and: "stub workManagementService.getWorkItem()"
		workManagementService.getWorkItem(_,_,_) >> {
		
			return new JsonSlurper().parseText(this.getClass().getResource('/testdata/parentData.json').text)
		}
		
		and: "stub workManagementService.updateItem()"
		
			workManagementService.getWorkItem(_,_,_) >> {
			String data = "${args[3]}"
			
			  assert(data.toString() == '[[op:test, path:/rev, value:2], [op:add, path:/fields/System.State, value:Active]]')
		}

		when: "calling method under test processADOData()"
		def resp = underTest.processADOData(adoMap)
		
		then: "Update should be made"
		resp == 'Update Succeeded'
	}
	
}

@TestConfiguration
@Profile("test")
@ComponentScan(["com.zions.vsts.services.work.WorkManagementService","com.zions.common.services.rest"])
@PropertySource("classpath:test.properties")
class ParentActivationMicroserviceTestConfig {
	def mockFactory = new DetachedMockFactory()
	
	@Value('${tfs.types}') 
	String wiTypes
	
	@Bean
	ParentActivationMicroService underTest() {
		return new ParentActivationMicroService()
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