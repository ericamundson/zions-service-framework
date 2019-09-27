package com.zions.vsts.services.release

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import com.zions.common.services.command.CommandManagementService
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
	private CommandManagementService commandManagementService
	
	@Autowired
	private ReleaseManagementService underTest
	/*
	@Test
	def 'ensureReleases with getRelease successflow' () {
		given: g_
		String json = this.getClass().getResource('/testdata/builddefinitions.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		8 * genericRestClient.get(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		projectManagementService.getProject(_, _, _) >> project
		//def template = new JsonSlurper().parseText(this.getClass().getResource('/testdata/templates.json').text)
		and: a_
		def team = new JsonSlurper().parseText(this.getClass().getResource('/testdata/teammembers.json').text)
		
		and: a_
		def repos = new JsonSlurper().parseText(this.getClass().getResource('/testdata/testrepos.json').text)
		codeManagementService.getRepos(_, _, _) >> repos
		
		and: a_
		genericRestClient.getTfsUrl() >> "visualstudio"
		
		when: w_
		def result = underTest.ensureReleases("visualstudioz", "DigitalBanking", '', '', '', team)
		
		then: t_
		result != null
	}
	
	@Test
	def 'ensureReleases with createRelease successflow' () {
		given: g_
		//8 * genericRestClient.get(_) >> null
		//and: a_
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		projectManagementService.getProject(_, _, _) >> project
		and: a_
		def repos = new JsonSlurper().parseText(this.getClass().getResource('/testdata/testrepos.json').text)
		codeManagementService.getRepos(_, _, _) >> repos
		
		def team = new JsonSlurper().parseText(this.getClass().getResource('/testdata/teammembers.json').text)
		def teamData = new JsonSlurper().parseText(this.getClass().getResource('/testdata/projectteam.json').text)
		memberManagementService.getTeam(_,_,_) >> teamData
		
		and: a_
		def buildDef = new JsonSlurper().parseText(this.getClass().getResource('/testdata/singlebuilddefination.json').text)
		buildManagementService.getBuild(_, _, _) >> buildDef
		
		and: a_
		def endpoint = new JsonSlurper().parseText(this.getClass().getResource('/testdata/serviceendpoints.json').text)
		endpointManagementService.getServiceEndpoint(_,_,_) >> endpoint
		
		and: a_
		genericRestClient.getTfsUrl() >> "visualstudio"
		def artifacts = [:]
		def template = new JsonSlurper().parseText(this.getClass().getResource('/testdata/singletemplate.json').text)
		//template << artifacts
		when: w_
		def result = underTest.ensureReleases("visualstudioz", "DigitalBanking", template, '', '', team)
		
		then: t_
		result != null
	}
	
	@Test
	def 'ensureReleaseFolder success flow' () {
		given: g_
		genericRestClient.getTfsUrl() >> "visualstudio"
		
		when: w_
		def result = underTest.ensureReleaseFolder('', 'DigitalBanking','C\\test\\folder')
		
		then: t_
		result != null
	}
	*/
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
	ReleaseManagementService underTest() {
		return new ReleaseManagementService();
	}
}