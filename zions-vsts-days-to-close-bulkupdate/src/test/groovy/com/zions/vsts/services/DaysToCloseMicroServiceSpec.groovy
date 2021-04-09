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
import spock.lang.Ignore


import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[DaysToCloseMicroserviceTestConfig])
class DaysToCloseMicroServiceSpec extends Specification {
	@Autowired
	DaysToCloseMicroService underTest;
	
	@Autowired
	WorkManagementService workManagementService;
	

	
	def "valid event for calculating daystoResolve Only"() {
		given: "A mock ADO event payload where work item has no parent"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/resolveOnly.json').text)
		//override work item and get work item details and return json file with valid bug count values
		and: "stub workManagementService.getWorkItem()"
		workManagementService.getWorkItem(_,_,_) >> {
		
			return new JsonSlurper().parseText(this.getClass().getResource('/testdata/resolveOnly.json').text)
		}
		
		//override update work item with test data values
		and: "stub workManagementService.updateItem()"
		
			workManagementService.updateWorkItem(_,_,_,_) >> {
			String data = "${args[3]}"
			
			  assert(data.toString() == '[[op:test, path:/rev, value:13], [op:add, path:/fields/Custom.DaysToResolve, value:6]]')
		}
		
		when: "ADO sends notification for work item change who's type is in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "Update should be made"
		resp == 'state change not applicable'
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
	IGenericRestClient genericRestClient() {
		//return new GenericRestClient('http://localhost:8080/ws', '', '')
		return mockFactory.Stub(GenericRestClient)
	}
}