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

@ContextConfiguration(classes=[PopulateChildMicroserviceTestConfig])
class PopulateChildMicroServiceSpec extends Specification {
	@Autowired
	PopulateChildMicroService underTest;
	
	@Autowired
	WorkManagementService workManagementService;
	
	//done
	def "No valid child types to update"() {
		given: "A mock ADO event payload exists for invalid child type"
		
		//epic json file with work items
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataInvalidChild.json').text)
		
		//figure out how to pull and iterate through child json files
				
		and: 'stub of inside get child data, get children of Epics (features)'
		
		def childData = new JsonSlurper().parseText(this.getClass().getResource('/testdata/childDataInvalidType.json').text)
		
		workManagementService.getChildren(_,_,_) >> childData
		
		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)
		

		then: "No Updates should be made"
		resp == 'no target children to update'
	}
	
	
	def "Parent field not populated"() {
		given: "A mock ADO event payload where parent work item field is not populated"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataFieldNotPopulated.json').text)
		
		
		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No updates should be made"
		resp == 'field not populated'
	}
	
	
	
	def "Child field matches parent field"() {
		given: "A mock ADO event payload where child work item field is already Populated"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataFieldAlreadyPopulated.json').text)
		

		and: 'stub of inside get child data, get children of Epics (features)'
		def childData = new JsonSlurper().parseText(this.getClass().getResource('/testdata/childDataNoUpdateNeeded.json').text)
		
		workManagementService.getChildren(_,_,_) >> childData
		
		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)
		

		then: "No Updates should be made"
		resp == 'no target children to update'
	}
	
	def "child does not exist for parent"() {
		given: "A mock ADO event payload where parent work item field is not populated"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataFieldNoChild.json').text)
		
		and: 'stub of inside get child data, get children of Epics (features)'
		def childData = new JsonSlurper().parseText(this.getClass().getResource('/testdata/emptychildData.json').text)
		
		  workManagementService.getChildren(_,_,_) >> childData
		
		  /*def emptyList = []
		  def childData = workManagementService.getChildren(_,_,_) << emptyList*/
		  
		 when: "ADO sends notification for work item change who's type is not in configured target list"
		 def resp = underTest.processADOData(adoMap)
		
		 then: "No updates should be made"
		 //1 * resp == 'child not present'
		 resp == 'child not present'
		
	}

	def "Successful child field update"() {
		given: "A mock ADO event payload exists that meets all criteria for update"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataEpicTwoChildren.json').text)
		
		and: 'stub of inside get child data, get children of Epics (features)'
		def childData = new JsonSlurper().parseText(this.getClass().getResource('/testdata/childData.json').text)
		
		workManagementService.getChildren(_,_,_) >> childData
		
		and: "stub workManagementService.updateItem()"
		workManagementService.batchWIChanges(_,_,_,_) >> { args ->
			
				
				def changes = args[2]
				//workManagementService.batchWIChanges(collection, project, changes, idMap)
				changes.each { change ->
					assert(change.body.toString() == '[[op:test, path:/rev, value:4], [op:add, path:/fields/Custom.OTLNumber, value:1000]]')
															
					//assert(change.toString() == '[[op:test, path:/rev, value:2], [op:add, path:/fields/Custom.OTLNumber, value:1000]]')
				}
				
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
class PopulateChildMicroserviceTestConfig {
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