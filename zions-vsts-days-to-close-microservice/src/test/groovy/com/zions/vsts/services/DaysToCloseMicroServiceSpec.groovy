package com.zions.vsts.services

import static org.junit.Assert.*

import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.SpockLabeler
import com.zions.vsts.services.tfs.rest.GenericRestClient
import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.work.calculations.CalculationManagementService

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
import spock.lang.Ignore
import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[DaysToCloseMicroserviceTestConfig])
class DaysToCloseMicroServiceSpec extends Specification {
	@Autowired
	DaysToCloseMicroService underTest;
	
	@Autowired
	WorkManagementService workManagementService;
	
	@Autowired
	CalculationManagementService calcManagementService
	
	
	def "handle null state values"() {
		given: "A mock ADO event payload exists for resetting reopen event"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/newJasonDataMissingOldValue.json').text)

		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No Updates should be made"
		resp == 'Error Retrieving previous state'

	
	}
	
	
	def "Not a valid type for days to close microservice"() {
		given: "A mock ADO event payload exists for invalid child state"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/invalidType.json').text)

		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No Updates should be made"
		resp == 'not a valid work item type'
	}
	
	
	def "no applicable changes made"() {
		given: "A mock ADO event payload exists for resetting reopen event"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/resetafterCount.json').text)

		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No Updates should be made"
		resp == 'no changes made to state'

	
	}
	
	def "valid event for resetting days to close"() {
		given: "A mock ADO event payload where work item has no parent"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/resetDaysToClose.json').text)
		//override work item and get work item details and return json file with valid bug count values
		and: "stub workManagementService.getWorkItem()"
		workManagementService.getWorkItem(_,_,_) >> {
		
			return new JsonSlurper().parseText(this.getClass().getResource('/testdata/resetDaysToClose.json').text)
		}
		
		//override update work item with test data values
		and: "stub workManagementService.updateItem()"
		
			workManagementService.updateWorkItem(_,_,_,_) >> {
			String data = "${args[3]}"
			
			  assert(data.toString() == '[[op:test, path:/rev, value:8], [op:add, path:/fields/Custom.DaysToClose, value:0]]')
		}
		
		when: "ADO sends notification for work item change who's type is in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "Update should be made"
		resp == 'Update Succeeded'
	}
		
	
	def "valid event for calculating days to close rounding down 6 days and 4 hours"() {
		given: "A mock ADO event payload where work item has no parent"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/validDaysToClose.json').text)
		//override work item and get work item details and return json file with valid bug count values
		and: "stub workManagementService.getWorkItem()"
		workManagementService.getWorkItem(_,_,_) >> {
		
			return new JsonSlurper().parseText(this.getClass().getResource('/testdata/validDaysToClose.json').text)
		}
		
		//override update work item with test data values
		and: "stub workManagementService.updateItem()"
		
			workManagementService.updateWorkItem(_,_,_,_) >> {
			String data = "${args[3]}"
			
			  assert(data.toString() == '[[op:test, path:/rev, value:13], [op:add, path:/fields/Custom.DaysToClose, value:6]]')
		}
		
		when: "ADO sends notification for work item change who's type is in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "Update should be made"
		resp == 'Update Succeeded'
	}
	
	def "valid event for calculating days to close rounding up 1497 days and 15 hours"() {
		given: "A mock ADO event payload where work item has no parent"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/validDaysToCloseRoundUp.json').text)
		//override work item and get work item details and return json file with valid bug count values
		and: "stub workManagementService.getWorkItem()"
		workManagementService.getWorkItem(_,_,_) >> {
		
			return new JsonSlurper().parseText(this.getClass().getResource('/testdata/validDaysToCloseRoundUp.json').text)
		}
		
		//override update work item with test data values
		and: "stub workManagementService.updateItem()"
		
			workManagementService.updateWorkItem(_,_,_,_) >> {
			String data = "${args[3]}"
			
			  assert(data.toString() == '[[op:test, path:/rev, value:7], [op:add, path:/fields/Custom.DaysToClose, value:1498]]')
		}
		
		when: "ADO sends notification for work item change who's type is in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "Update should be made"
		resp == 'Update Succeeded'
	}
	
	
	def "valid event for calculating same day closures of 0.5"() {
		given: "A mock ADO event payload where work item has no parent"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/sameDayClosure.json').text)
		//override work item and get work item details and return json file with valid bug count values
		and: "stub workManagementService.getWorkItem()"
		workManagementService.getWorkItem(_,_,_) >> {
		
			return new JsonSlurper().parseText(this.getClass().getResource('/testdata/sameDayClosure.json').text)
		}
		
		//override update work item with test data values
		and: "stub workManagementService.updateItem()"
		
			workManagementService.updateWorkItem(_,_,_,_) >> {
			String data = "${args[3]}"
			
			  assert(data.toString() == '[[op:test, path:/rev, value:2], [op:add, path:/fields/Custom.DaysToClose, value:0.5]]')
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
class DaysToCloseMicroserviceTestConfig {
	def mockFactory = new DetachedMockFactory()
	
	@Value('${tfs.types}') 
	String wiTypes
	
	@Bean
	DaysToCloseMicroService underTest() {
		return new DaysToCloseMicroService()
	}
	@Bean
	WorkManagementService workManagementService() {
		//return mockFactory.Mock(WorkManagementService)
		return mockFactory.Stub(WorkManagementService)
	}
	
	@Bean
	CalculationManagementService calcManagementService() {
		//return mockFactory.Mock(WorkManagementService)
		return mockFactory.Stub(CalculationManagementService)
	}
	
	@Bean
	IGenericRestClient genericRestClient() {
		//return new GenericRestClient('http://localhost:8080/ws', '', '')
		return mockFactory.Stub(GenericRestClient)
	}
}