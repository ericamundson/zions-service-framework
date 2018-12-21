package com.zions.vsts.services.release

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.admin.project.ProjectManagementServiceTestConfig
import com.zions.vsts.services.build.BuildManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.endpoint.EndpointManagementService
import com.zions.vsts.services.permissions.PermissionsManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient
import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.mock.DetachedMockFactory

/**
 * Variety of test case for unit testing MemberManagementService
 *
 * @author v072160 
 *
 */
@ContextConfiguration(classes=[ReleaseManagementServiceTestConfig])
class ReleaseManagementServiceSpecTest extends Specification {
	
	@Autowired
	private IGenericRestClient genericRestClient;
	
	@Autowired
	private ProjectManagementService projectManagementService
	
	@Autowired
	private CodeManagementService codeManagementService
	
	@Autowired
	private BuildManagementService buildManagementService
	
	@Autowired
	private MemberManagementService memberManagementService

	@Autowired
	private EndpointManagementService endpointManagementService
	
	@Autowired
	private ReleaseManagementService underTest
	
	/*@Test
	def 'getRelease successflow'() {
		
		given:
		String json = this.getClass().getResource('/testdata/definations.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when:
			def result = underTest.getRelease('', project, '')
			
		then:
			result != null
	}*/
		
}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class ReleaseManagementServiceTestConfig {
	def mockFactory = new DetachedMockFactory()
	
	@Bean
	IGenericRestClient genericRestClient() {
		return mockFactory.Mock(GenericRestClient, name: 'genericRestClient')
	}
	
	@Bean
	BuildManagementService buildManagementService() {
		return mockFactory.Mock(BuildManagementService);
	}
	
	@Bean
	CodeManagementService codeManagementService() {
		return mockFactory.Mock(CodeManagementService);
	}
	
	@Bean
	ProjectManagementService projectManagementService() {
		return mockFactory.Mock(ProjectManagementService);
	}
	
	@Bean
	EndpointManagementService endpointManagementService() {
		return mockFactory.Mock(EndpointManagementService);
	}
	
	@Bean
	MemberManagementService memberManagementService() {
		return mockFactory.Mock(MemberManagementService);
	}
	
	@Bean
	PermissionsManagementService permissionsManagementService() {
		return mockFactory.Mock(PermissionsManagementService);
	}
	
	@Bean
	ReleaseManagementService underTest() {
		return new ReleaseManagementService();
	}
}