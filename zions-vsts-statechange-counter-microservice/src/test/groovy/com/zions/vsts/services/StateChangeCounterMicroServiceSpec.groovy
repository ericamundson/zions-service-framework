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

@ContextConfiguration(classes=[StateChangeCounterMicroserviceTestConfig])
class StateChangeCounterMicroServiceSpec extends Specification {
	@Autowired
	StateChangeCounterMicroService underTest;
	
	@Autowired
	WorkManagementService workManagementService;
	
	def "Not a valid type for state change counter"() {
		given: "A mock ADO event payload exists for invalid child state"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/invalidType.json').text)

		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No Updates should be made"
		resp == 'not a valid work item type'
	}
	
	def "Reset after counting a reopen event"() {
		given: "A mock ADO event payload exists for resetting reopen event"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/resetafterCount.json').text)

		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No Updates should be made"
		resp == 'no changes made to state'

	
	}
	
	def "Non applicable state change events should not be counted"() {
		given: "A mock ADO event payload exists for resetting reopen event"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataWrongState.json').text)

		
		and: "stub workManagementService.getWorkItem()"
		workManagementService.getWorkItem(_,_,_) >> {
		
			return new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataWrongState.json').text)
		}
		
		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No Updates should be made"
		resp == 'state change not applicable'

	
	}
	

	def "valid state change event for ReOpen Counter"() {
		given: "A mock ADO event payload where work item has no parent"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/validbugCount.json').text)
		//override work item and get work item details and return json file with valid bug count values
		and: "stub workManagementService.getWorkItem()"
		workManagementService.getWorkItem(_,_,_) >> {
		
			return new JsonSlurper().parseText(this.getClass().getResource('/testdata/validbugCount.json').text)
		}
		
		//override update work item with test data values
		and: "stub workManagementService.updateItem()"
		
			workManagementService.updateWorkItem(_,_,_,_) >> {
			String data = "${args[3]}"
			
			  assert(data.toString() == '[[op:test, path:/rev, value:79], [op:add, path:/fields/Custom.ReOpenCounter, value:9]]')
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
class StateChangeCounterMicroserviceTestConfig {
	def mockFactory = new DetachedMockFactory()
	
	@Value('${tfs.types}') 
	String wiTypes
	
	@Bean
	StateChangeCounterMicroService underTest() {
		return new StateChangeCounterMicroService()
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