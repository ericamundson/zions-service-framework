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
import com.zions.common.services.test.SpockLabeler
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
	
	def 'ensureReleases with getRelease successflow' () {
		given:
		String json = this.getClass().getResource('/testdata/builddefinitions.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		8 * genericRestClient.get(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		projectManagementService.getProject(_, _, _) >> project
		//def template = new JsonSlurper().parseText(this.getClass().getResource('/testdata/templates.json').text)
		and:
		def team = new JsonSlurper().parseText(this.getClass().getResource('/testdata/teammembers.json').text)
		
		and:
		def repos = new JsonSlurper().parseText(this.getClass().getResource('/testdata/testrepos.json').text)
		codeManagementService.getRepos(_, _, _) >> repos
		
		and:
		genericRestClient.getTfsUrl() >> "visualstudio"
		
		when:
		def result = underTest.ensureReleases("visualstudioz", "DigitalBanking", '', '', '', team)
		
		then:
		result != null
	}
	
	
	def 'ensureReleases with createRelease successflow' () {
		given:
		//8 * genericRestClient.get(_) >> null
		//and:
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		projectManagementService.getProject(_, _, _) >> project
		and:
		def repos = new JsonSlurper().parseText(this.getClass().getResource('/testdata/testrepos.json').text)
		codeManagementService.getRepos(_, _, _) >> repos
		
		def team = new JsonSlurper().parseText(this.getClass().getResource('/testdata/teammembers.json').text)
		def teamData = new JsonSlurper().parseText(this.getClass().getResource('/testdata/projectteam.json').text)
		memberManagementService.getTeam(_,_,_) >> teamData
		
		and:
		def buildDef = new JsonSlurper().parseText(this.getClass().getResource('/testdata/singlebuilddefination.json').text)
		buildManagementService.getBuild(_, _, _) >> buildDef
		
		and:
		def endpoint = new JsonSlurper().parseText(this.getClass().getResource('/testdata/serviceendpoints.json').text)
		endpointManagementService.getServiceEndpoint(_,_,_) >> endpoint
		
		and:
		genericRestClient.getTfsUrl() >> "visualstudio"
		def artifacts = [:]
		def template = new JsonSlurper().parseText(this.getClass().getResource('/testdata/singletemplate.json').text)
		//template << artifacts
		when:
		def result = underTest.ensureReleases("visualstudioz", "DigitalBanking", template, '', '', team)
		
		then:
		result != null
	}
	
	
	def 'ensureReleaseFolder success flow' () {
		given:
		genericRestClient.getTfsUrl() >> "visualstudio"
		
		when:
		def result = underTest.ensureReleaseFolder('', 'DigitalBanking','C\\test\\folder')
		
		then:
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