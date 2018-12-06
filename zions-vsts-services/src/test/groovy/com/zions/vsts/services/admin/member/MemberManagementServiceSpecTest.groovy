package com.zions.vsts.services.admin.member

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.admin.project.ProjectManagementServiceTestConfig
import com.zions.vsts.services.tfs.rest.GenericRestClient
import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.mock.DetachedMockFactory

/**
 * Variety of test case for unit testing MemberManagementService
 * 
 * @author z091182
 *
 */
@ContextConfiguration(classes=[MemberManagementServiceTestConfig])
class MemberManagementServiceSpecTest extends Specification {
	
	@Autowired
	IGenericRestClient genericRestClient
	
	@Autowired
	ProjectManagementService projectManagementService
	
	@Autowired
	MemberManagementService underTest
	
	def 'addMemberToTeams test success flow.'() {
		given: 'stub project management service ensureTeam'
		def teamInfo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/projectteam.json').text)
		1 * projectManagementService.ensureTeam(_, _, _) >> teamInfo
		
		and: 'stub azure call to get entitlement.'
		def entitlements = new JsonSlurper().parseText(this.getClass().getResource('/testdata/memberentitlements.json').text)
		1 * genericRestClient.get(_) >> entitlements
		
		and: 'stub azure devops request adding member to a team'
		1 * genericRestClient.post(_) >> [:]
		
		when: 'call method (addMemberToTeams) under test.'
		def teamList = new JsonSlurper().parseText(this.getClass().getResource('/testdata/oneteam.json').text)
		def res = underTest.addMemberToTeams("", 'Matthew.Holbrook2@zionsbancorp.com', teamList.value)
		
		then: 'validate member added to team'
		res == null
	}
	
	def 'addMemberToTeams test success flow and member not current user.'() {
		given: 'stub project management service ensureTeam'
		def teamInfo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/projectteam.json').text)
		1 * projectManagementService.ensureTeam(_, _, _) >> teamInfo
		
		and: 'stub azure call to get entitlement.'
		def entitlements = new JsonSlurper().parseText(this.getClass().getResource('/testdata/memberentitlements.json').text)
		1 * genericRestClient.get(_) >> entitlements
		
		and: 'stub azure devops request adding member to a team'
		1 * genericRestClient.post(_) >> [:]
		
		and: 'stub azure call to get entitlement with John Milman'
		def entitlements2 = new JsonSlurper().parseText(this.getClass().getResource('/testdata/memberentitlements2.json').text)
		1 * genericRestClient.get(_) >> entitlements2
		
		and: 'stub azure call to patch entitlements to stakeholder'
		1 * genericRestClient.patch(_) >> [:]

		when: 'call method (addMemberToTeams) under test.'
		def teamList = new JsonSlurper().parseText(this.getClass().getResource('/testdata/oneteam.json').text)
		def res = underTest.addMemberToTeams("", 'John.Milman@zionsbancorp.com', teamList.value)
		
		then: 'validate member added to team'
		res == null
	}
	
	def 'getProjectMembersMap test success flow.'() {
		given: 'stub project management service getProject'
		def projectInfo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		1 * projectManagementService.getProject(_, _) >> projectInfo
		
		and: 'stub azure devops request to get all teams'
		def teamsInfo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/oneteam.json').text)
		1 * genericRestClient.get(_) >> teamsInfo
		
		and: 'stub azure devops get team members call'
		def membersInfo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/teammembers.json').text)
		1 * genericRestClient.get(_) >> membersInfo
		
		when: 'call method (getProjectMembersMap) under test.'
		def members = underTest.getProjectMembersMap("", 'DigitalBanking')
		
		then: 'validate member map'
		members.size() == 43
	}
	
	/**
	 * 
	 * @return
	 */
	def 'getTeam test success flow.'() {
		given:  'stub azure devops team request'
		def team = new JsonSlurper().parseText(this.getClass().getResource('/testdata/projectteam.json').text)
		1 * genericRestClient.get(_) >> team
		
		when: 'call method under test (getTeam)'
		def projectInfo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def teamInfo = underTest.getTeam("", projectInfo, 'OB')
		
		then: 'Validate team name'
		"${teamInfo.name}" == 'OB'
	}
}

@TestConfiguration
@Profile("test")
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
	MemberManagementService underTest() {
		return new MemberManagementService();
	}
	
}