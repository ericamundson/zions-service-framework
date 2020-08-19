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
	def "Not a valid parent work item type"() {
		given: "A mock ADO event payload exists for wrong work item type"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataInvalidParentType.json').text)

		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No updates should be made"
		resp == 'not a valid work item type'
}
	
	//capture new files? //attempt 1
	def "Work item changes not applicable"() {
		given: "A mock ADO event payload where parent work item field is not populated"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataFieldNotPopulated.json').text)
		
		and: 'stub of inside get child data, get children of Epics (features)'
		def childData = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adaDataChildNA.json').text)
		
		  workManagementService.getChildren(_,_,_) >> childData
		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No updates should be made"
		resp == 'work item changes do not apply'
		//resp == 'no update needed'
		
	}
	

	
	//not working
	def "Successful child field update"() {
		given: "A mock ADO event payload exists that meets all criteria for update"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/parentDataArray.json').text)
		
		and: 'stub of inside get child data, get children of Epics (features)'
		def childData = new JsonSlurper().parseText(this.getClass().getResource('/testdata/childDataArray.json').text)
		
		workManagementService.getChildren(_,_,_) >> childData
		
		and: "stub workManagementService.updateItem()"
		workManagementService.batchWIChanges(_,_,_,_) >> { args ->
			
				
				def changes = args[2]
				//workManagementService.batchWIChanges(collection, project, changes, idMap)
				changes.each { change ->
					
					
					//assert(change.body.toString() == '[[op:test, path:/rev, value:19], [op:add, path:/fields/Custom.OTLNumber, value:600], [op:add, path:/fields/Custom.ExternalID, value:601]]')
					//assert(change.body.toString() == '[[op:test, path:/rev, value:19], [op:add, path:/fields/Custom.OTLNumber, value:600], [op:add, path:/fields/Custom.ExternalID, value:601]]')

					
					//assert(change.body.toString() == '[[op:test, path:/rev, value:72], [op:add, path:/fields/Custom.OTLNumber, value:600]]')
					//assert(change.body.toString() == '[[op:test, path:/rev, value:72], [op:add, path:/fields/Custom.ExternalID, value:601]]')
													  

					
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