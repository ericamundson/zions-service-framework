package com.zions.vsts.services.setowner

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

@ContextConfiguration(classes=[SetOwnerMicroserviceTestConfig])
class SetOwnerMicroserviceSpec extends Specification {
	@Autowired
	SetOwnerMicroService underTest
	@Autowired
	WorkManagementService workManagementService

	def "Not a configured target work item type"() {
		given: "A mock ADO event payload exists for wrong work item type"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataFileWrongType.json').text)

		when: "ADO sends notification for work item change who's type is not in configured target list"
		def resp = underTest.processADOData(adoMap)

		then: "No updates should be made"
		resp == false
	}
	
	def "Successful Assignment to Parent Owner"() {
		given: "A mock ADO event payload exists that meets all criteria for update"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/adoDataSuccessfulAssignment.json').text)

		when: "ADO sends notification for work item change"
		def resp = underTest.processADOData(adoMap)
		1 * workManagementService.getWorkItem(_) >> {
			return new JsonSlurper().parseText(this.getClass().getResource('/testdata/parentData.json').text)
		}
		1 * workManagementService.updateWorkItem(_) >> { args ->
			String data = "${args[3]}"
			println(data.toString())
			assert(data.toString() == '[{op=test, path=/rev, value=2}, {op=add, path=/fields/System.AssignedTo, value=robert.huet@zionsbancorp.com}]')
		}
		
		then: "No updates should be made"
		resp == false
	}
	
}

@TestConfiguration
@Profile("test")
@ComponentScan(["com.zions.vsts.services.work.WorkManagementService","com.zions.common.services.rest"])
@PropertySource("classpath:test.properties")
class SetOwnerMicroserviceTestConfig {
	def mockFactory = new DetachedMockFactory()

	@Autowired
	@Value('${tfs.types}') 
	String types
	@Bean
	SetOwnerMicroService underTest() {
		return new SetOwnerMicroService()
	}
	@Bean
	WorkManagementService workManagementService() {
		return mockFactory.Mock(WorkManagementService)
	}
	@Bean
	IGenericRestClient genericRestClient() {
		return new GenericRestClient('http://localhost:8080/ws', '', '')
	}
}