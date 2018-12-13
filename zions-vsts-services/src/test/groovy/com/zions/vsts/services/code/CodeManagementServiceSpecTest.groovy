package com.zions.vsts.services.code

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
@ContextConfiguration(classes=[CodeManagementServiceTestConfig])
class CodeManagementServiceSpecTest extends Specification {
	
	@Autowired
	private IGenericRestClient genericRestClient;
	
	@Autowired
	private ProjectManagementService projectManagementService
	
	@Autowired
	private EndpointManagementService endpointManagementService
	
	@Autowired
	private MemberManagementService memberManagementService
	
	@Autowired
	private PermissionsManagementService permissionsManagementService
	
	@Autowired
	private CodeManagementService underTest
	
	@Test
	def 'getRepos success flow with two params' () {
		
		given:
		String json = this.getClass().getResource('/testdata/repos.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		//1 * genericRestClient.get(_) >> project
		
		when:
		def result = underTest.getRepos("eto-dev", project)
		
		then:
		"${result.count}" == "4"
	}
	
	@Test
	def 'getRepos success flow with three parms' () {
		given:
		
		String json = this.getClass().getResource('/testdata/repos.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		//1 * genericRestClient.get(_) >> project
		
		def team = new JsonSlurper().parseText(this.getClass().getResource('/testdata/projectteam.json').text)
		//1 * genericRestClient.get(_) >> team
		
		when:
		def result = underTest.getRepos("eto-dev", project, team)
		
		then:
		"${result.size}" == "0"
	}
	
	@Test
	def 'listTopLevel success flow' () {
		given:
		String json = this.getClass().getResource('/testdata/items.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		def repos = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repos.json').text)
		
		when:
		def result = underTest.listTopLevel("eto-dev", project, repos)
		
		then:
		"${result.count}" == "1"
		
	}
	
	@Test
	def 'getBuildPropertiesFile success flow' () {
		given:
		String json = this.getClass().getResource('/testdata/items.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		def repos = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repos.json').text)
		
		when:
		def result = underTest.getBuildPropertiesFile("eto-dev", project, repos,'')
		
		then:
		"${result.count}" == "1"
	}
	
	@Test
	def 'getAuthUrl success flow' () {
		when:
		def out = underTest.getAuthUrl('http://dev.azure.com', 'abc', 'abc')
		
		then:
		out == "https://abc:abc@ev.azure.com"
	}
	
	/*@Test
	def 'importRepoDir success flow' () {
		when:
		def out = underTest.importRepoDir('', 'DigitalBanking', '', null, '', '')
		
		then:
		out != null
	}
	
	@Test
	def 'importRepoCLI success flow' () {
		when:
		def out = underTest.importRepoCLI('', 'DigitalBanking', '', '', '', '')
		
		then:
		out != null
	}*/
	
	@Test
	def 'getRefHead success flow' () {
		
		given:
		String json = this.getClass().getResource('/testdata/refs.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		def repos = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repos.json').text)
		
		when:
		def result = underTest.getRefHead('', project, repos)
		
		then:
		"${result.name}" == "refs/heads/master"
	}
	
	@Test
	def 'createDeployManifest success flow' () {
		
		given:
		String json = this.getClass().getResource('/testdata/pushes.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		def repos = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repos.json').text)
		
		when:
		def result = underTest.createDeployManifest('', project, repos)
		
		then:
		result == null
	
	}
		
}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class CodeManagementServiceTestConfig {
	def mockFactory = new DetachedMockFactory()
	
	@Bean
	IGenericRestClient genericRestClient() {
		return mockFactory.Mock(GenericRestClient, name: 'genericRestClient')
	}
	
	@Bean
	CodeManagementService underTest() {
		return new CodeManagementService() ;
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
	
}