package com.zions.vsts.services.mapfield

import static org.junit.Assert.*

import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.SpockLabeler
import com.zions.vsts.services.mapfield.ProjectProperties
import com.zions.vsts.services.asset.SharedAssetService
import com.zions.vsts.services.mapfield.MapFieldMicroService
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

@ContextConfiguration(classes=[MapFieldMicroserviceTestConfig])
class MapFieldMicroServiceSpec extends Specification {
	@Autowired
	MapFieldMicroService underTest;
	
	@Autowired
	ProjectProperties projectProperties
	
	@Autowired
	WorkManagementService workManagementService;

	/***PLEASE NOTE****These tests are based on the impact mapping table which applies to the Workaround work item type
	 * The impact map uses a json file to map the values in the fields Custom.WorkaroundComplexity and Custom.Frequency
	 * and determines the Impact value based on the mapping*/
	
	/***These tests also mock the values provided in the application.yaml file-since the spock framework cannot easily
	 * pull in data in yaml files processed by a ProjectProperties class - lines 54-57 for the first test are good 
	 * examples of how to mock property file data
	 * 
	 */

	def "Complexity and Frequency are set, but output is unassigned"() {
		
		given: "A mock ADO event payload where Assigned To is already set"
		def adoEvent = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataMissingOutput.json').text)
		
				
		and: "stub input1 and input2 values for testing"
		def list1 = ['Custom.WorkaroundComplexity', 'Custom.Frequency']
		String outputTest = 'Custom.Impact'
		projectProperties.inputFields >>  new ArrayList<String>(list1)
		projectProperties.outputField >>  outputTest
				
		and: "stub workManagementService.updateItem()"
		workManagementService.updateWorkItem(_,_,_,_) >> { args ->
			
			
			String data = "${args[3]}"
			
			// Inject mapped outs here to test full output map
			assert(data.toString() == "[[op:test, path:/rev, value:127], [op:add, path:/fields/$outputTest, value:$Output]]")
					
		}

		
		when: "ADO sends notification with set Complexity and Frequency"
		adoEvent.resource.revision.fields.'Custom.WorkaroundComplexity' = Input1
		adoEvent.resource.revision.fields.'Custom.Frequency' = Input2
		def resp = underTest.processADOData(adoEvent)

		then: "Output is updated to the expected value"
		
		resp == "Output value updated to: $Output"
				
		where:
		
		Input1 | Input2 | Output
		"Low" | "Infrequent" | "Low"
		"Low" | "Monthly" | "Low"
		"Low" | "Daily" | "Low"
		"Medium" | "Monthly" | "Medium"
		"Medium" | "Infrequent" | "Medium"
		"Medium" | "Daily" | "High"
		"High" | "Infrequent" | "Medium"
		"High" | "Daily" | "High"
		"High" | "Monthly" | "High"

	}
	
	
	def "Complexity and Frequency are set and Output value is wrong"() {
		given: "A mock ADO event payload where work item has no parent"
		def adoEvent = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataWrongOutput.json').text)
		
		and: "stub input1 and input2 values for testing"
		def list1 = ['Custom.WorkaroundComplexity', 'Custom.Frequency']
		projectProperties.inputFields >>  new ArrayList<String>(list1)
		
		and: "stub workManagementService.updateItem()"
		workManagementService.updateWorkItem(_,_,_,_) >> { args ->
			String data = "${args[3]}"
			assert(data.toString() == '[[op:test, path:/rev, value:134], [op:add, path:/fields/, value:Low]]')
		}

		
		when: "ADO sends notification"
		def resp = underTest.processADOData(adoEvent)

		then: "Output is updated to the expected value"
		resp == "Output value updated to: Low"
	}

	
	def "Complexity and Frequency are set and Impact is set to correct value-No Updates"() {
		given: "A mock ADO event payload for WI having unassigned parent"
		def adoEvent = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataNoChange.json').text)
		
		and: "stub input1 and input2 values for testing"
		def list1 = ['Custom.WorkaroundComplexity', 'Custom.Frequency']
		String outputTest = 'Custom.Impact'
		projectProperties.inputFields >>  new ArrayList<String>(list1)
		projectProperties.outputField >>  outputTest
				
		when: "ADO sends notification"
	
		def resp = underTest.processADOData(adoEvent)

		then: "No updates should be made"
		resp == 'No updates needed'
	}
	

	
	def loadImpactmap() {
		def resource = new JsonSlurper().parseText(this.getClass().getResource('/mapping/impactmap.json').text)
		return resource.value
	}
}

@TestConfiguration
@Profile("test")
@ComponentScan(["com.zions.vsts.services.work.WorkManagementService","com.zions.common.services.rest"])
@PropertySource("classpath:test.properties")
//@PropertySource("classpath:test.yaml")
class MapFieldMicroserviceTestConfig {
	def mockFactory = new DetachedMockFactory()
	
	@Value('${tfs.types}') 
	String wiTypes
	
	/*@Bean
	ProjectProperties projectProperties() {
		return new ProjectProperties()
	}*/
	
	@Bean
	ProjectProperties projectProperties() {
		return mockFactory.Stub(ProjectProperties)
	}
	
	@Bean
	MapFieldMicroService underTest() {
		return new MapFieldMicroService()
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