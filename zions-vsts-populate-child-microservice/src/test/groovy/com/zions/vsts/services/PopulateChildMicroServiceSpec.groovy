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
class PopulateChildMicroServiceSpec extends Specification {
	@Autowired
	PopulateChildMicroService underTest;
	
	@Autowired
	WorkManagementService workManagementService;
	
	def "Successful Population of Child Fields"() {
		given: "A mock ADO event payload exists that meets all criteria for update"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataEpicTwoChildren.json').text)

		and: "stub workManagementService.getWorkItem()"
		workManagementService.getWorkItem(_,_,_) >> {
		
			return new JsonSlurper().parseText(this.getClass().getResource('/testdata/childData.json').text)
		}
		
		and: "stub workManagementService.updateItem()"
		
			workManagementService.getWorkItem(_,_,_) >> {
			String data = "${args[3]}"
			
			  
			  assert(data.toString() == '[[op:test  paht:/rev, value:2], [op:add, path:/fields/Custom.OTLNumber, value: "$otlField"]]')
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
	PopulateChildMicroService underTest() {
		return new PopulateChildMicroService()
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