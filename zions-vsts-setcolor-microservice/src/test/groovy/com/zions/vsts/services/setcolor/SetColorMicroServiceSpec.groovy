package com.zions.vsts.services.setcolor

import static org.junit.Assert.*

import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.SpockLabeler
import com.zions.vsts.services.asset.SharedAssetService
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

@ContextConfiguration(classes=[SetColorMicroserviceTestConfig])
class SetColorMicroServiceSpec extends Specification {
	@Autowired
	SetColorMicroService underTest;
	
	@Autowired
	WorkManagementService workManagementService;

	@Autowired
	SharedAssetService sharedAssetService;
	
	def "Not a Bug work item type"() {
		given: "A mock ADO event payload exists for wrong work item type"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataWrongType.json').text)

		and: "stub sharedAssetService.getAsset()"
		sharedAssetService.getAsset(_) >> loadColormap()
		
		when: "ADO sends notification"
		def resp = underTest.processADOData(adoMap)

		then: "No updates should be made"
		resp == 'Not a Bug'
	}
	
	def "No change to Severity, Priority or Color"() {
		given: "A mock ADO event payload where state is not Closed"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataNoRelevantChanges.json').text)

		and: "stub sharedAssetService.getAsset()"
		sharedAssetService.getAsset(_) >> loadColormap()
		
		when: "ADO sends notification"
		def resp = underTest.processADOData(adoMap)

		then: "No updates should be made"
		resp == 'No change to Severity, Priority or Color'
	}
	
	def "Severity and Priority are set, but Color is unassigned"() {
		given: "A mock ADO event payload where Assigned To is already set"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataMissingColor.json').text)
		
		and: "stub workManagementService.updateItem()"
		workManagementService.updateWorkItem(_,_,_,_) >> { args ->
			String data = "${args[3]}"
			// Inject mapped color here to test full color map
			assert(data.toString() == "[[op:test, path:/rev, value:20], [op:add, path:/fields/Custom.Color, value:$Color]]")
		}

		and: "stub sharedAssetService.getAsset()"
		sharedAssetService.getAsset(_) >> loadColormap()
		
		when: "ADO sends notification with set Priority and Severity"
		adoMap.resource.revision.fields.'Microsoft.VSTS.Common.Priority' = Priority
		adoMap.resource.revision.fields.'Microsoft.VSTS.Common.Severity' = Severity
		def resp = underTest.processADOData(adoMap)

		then: "Color is updated to the expected color"
		resp == 'Color updated'
		
		where:
		Priority | Severity | Color
		1 | "1 - Critical" | "Red"
		1 | "2 - High" | "Red"
		1 | "3 - Medium" | "Red"
		1 | "4 - Low" | "Red"
		2 | "1 - Critical" | "Red"
		2 | "2 - High" | "Red"
		2 | "3 - Medium" | "Yellow"
		2 | "4 - Low" | "Yellow"
		3 | "1 - Critical" | "Yellow"
		3 | "2 - High" | "Yellow"
		3 | "3 - Medium" | "Green"
		3 | "4 - Low" | "Green"
		4 | "1 - Critical" | "Yellow"
		4 | "2 - High" | "Green"
		4 | "3 - Medium" | "Green"
		4 | "4 - Low" | "Green"

	}
	
	def "Severity and Priority are set, and Color is wrong"() {
		given: "A mock ADO event payload where work item has no parent"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataWrongColor.json').text)
		
		and: "stub workManagementService.updateItem()"
		workManagementService.updateWorkItem(_,_,_,_) >> { args ->
			String data = "${args[3]}"
			assert(data.toString() == '[[op:test, path:/rev, value:35], [op:add, path:/fields/Custom.Color, value:Red]]')
		}

		and: "stub sharedAssetService.getAsset()"
		sharedAssetService.getAsset(_) >> loadColormap()
		
		when: "ADO sends notification"
		def resp = underTest.processADOData(adoMap)

		then: "Color is updated to the expected color"
		resp == 'Color updated'
	}

	def "Severity and Priority are set, and Color is correct"() {
		given: "A mock ADO event payload for WI having unassigned parent"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataCorrectColor.json').text)
		
		and: "stub sharedAssetService.getAsset()"
		sharedAssetService.getAsset(_) >> loadColormap()
		
		when: "ADO sends notification"
		def resp = underTest.processADOData(adoMap)

		then: "No updates should be made"
		resp == 'No updates needed'
	}
	
	def "Either Severity or Priority is unassigned, but Color is set"() {
		given: "A mock ADO event payload exists that meets all criteria for update"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataPrematureColor.json').text)

		and: "stub workManagementService.updateItem()"
		workManagementService.updateWorkItem(_,_,_,_) >> { args ->
			String data = "${args[3]}"
			assert(data.toString() == '[[op:test, path:/rev, value:34], [op:add, path:/fields/Custom.Color, value:]]')
		}

		and: "stub sharedAssetService.getAsset()"
		sharedAssetService.getAsset(_) >> loadColormap()
		
		when: "ADO sends notification"
		def resp = underTest.processADOData(adoMap)
		
		then: "Color should be updated to unassigned"
		resp == 'Color set to unassigned'
	}
	
	def loadColormap() {
		def resource = new JsonSlurper().parseText(this.getClass().getResource('/testdata/colormap.json').text)
		return resource
	}
}

@TestConfiguration
@Profile("test")
@ComponentScan(["com.zions.vsts.services.work.WorkManagementService","com.zions.common.services.rest"])
@PropertySource("classpath:test.properties")
class SetColorMicroserviceTestConfig {
	def mockFactory = new DetachedMockFactory()
	
	@Value('${tfs.types}') 
	String wiTypes
	
	@Bean
	SetColorMicroService underTest() {
		return new SetColorMicroService()
	}
	@Bean
	SharedAssetService sharedAssetService() {
		return mockFactory.Stub(SharedAssetService)
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