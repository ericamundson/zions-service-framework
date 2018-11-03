package com.zions.vsts.services.admin.member

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.admin.project.ProjectManagementServiceTestConfig
import com.zions.vsts.services.tfs.rest.GenericRestClient

import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[MemberManagementServiceTestConfig])
class MemberManagementServiceSpecTest extends Specification {
	
	@Autowired
	IGenericRestClient genericRestClient
	
	@Autowired
	ProjectManagementService projectManagementService
	
	@Autowired
	MemberManagementService memberManagementService
	
	def 'addMemberToTeams test success flow.'() {
		given: 'stub project management service ensureTeam'
		
		and: 'stub azure devops request adding member to a team'
		
		when: 'call method (addMemberToTeams) under test.'
		
		then: 'validate member added to team'
		
	}

}

@TestConfiguration
@PropertySource("classpath:test.properties")
class MemberManagementServiceTestConfig {
	def mockFactory = new DetachedMockFactory()
	
		@Bean
		IGenericRestClient genericRestClient() {
			return mockFactory.Mock(GenericRestClient, name: 'genericRestClient')
		}
		@Bean
		ProjectManagementService projectManagementService() {
			return mockFactory.Mock(ProjectManagementService);
		}
		
		@Bean
		MemberManagementService memberManagementService() {
			return new MemberManagementService();
		}
	
}