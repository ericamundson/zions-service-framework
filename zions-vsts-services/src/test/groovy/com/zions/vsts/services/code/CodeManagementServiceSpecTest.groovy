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
	private IGenericRestClient genericRestClient
	
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
		def out = underTest.getAuthUrl('https://dev.azure.com', 'v072160', 'K@nakadurga@2171')
		
		then:
		out == "https://v072160:K%40nakadurga%402171@dev.azure.com"
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

	@Test
	def 'createRepo success flow' () {
		
		given:
		String json = this.getClass().getResource('/testdata/repos.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.post(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when:
		def result = underTest.createRepo('',project,'DigitalBanking')
		
		then:
		"${result.count}" == "4"
	
	}
	
	@Test
	def 'getRepo success flow' () {
		
		given:
		String json = this.getClass().getResource('/testdata/DigitalBanking.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when:
		def result = underTest.getRepo('',project,'DigitalBanking')
		
		then:
		"${result.id}" == "26862cc3-6775-4676-bb22-3bad625dcaa7"
	
	}
	
	@Test
	def 'getDeployManifest success flow' () {
		
		given:
		String json = this.getClass().getResource('/testdata/items.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		
		when:
		def result = underTest.getDeployManifest('',project,repo)
		
		then:
		result ==null
	
	}
	
	@Test
	def 'importRepo success flow' () {
		
		given:
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		1 * projectManagementService.getProject(_, _ ) >> project
		
		and:
		def endpoint = new JsonSlurper().parseText(this.getClass().getResource('/testdata/serviceendpoints.json').text)
		1 * endpointManagementService.createServiceEndpoint(_,_,_,_,_) >> endpoint
	
		and:
		String json = this.getClass().getResource('/testdata/importRequests.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		2 * genericRestClient.post(_) >> out
		
		when:
		def result = underTest.importRepo('', 'DigitalBanking', 'DigitalBanking', '', '', '')
		
		then:
		"${result.count}" =="0"
	
	}
	
	@Test
	def 'importRepoDir NullPointerException flow' () {
		given:
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		1 * projectManagementService.getProject(_, _ ) >> project
		when:
		def result = underTest.importRepoDir('', 'DigitalBanking', 'DigitialBanking', null, 'v072160', 'K@nakadurga@2171')
		
		then:
		thrown(NullPointerException)
	
	}
	
	@Test
	def 'importRepoCLI NullPointerException flow' () {
		given:
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		1 * projectManagementService.getProject(_, _ ) >> project
		
		when:
		def result = underTest.importRepoCLI('', 'DigitalBanking', 'DigitialBanking', null, 'v072160', 'K@nakadurga@2171')
		
		then:
		thrown(NullPointerException)
	
	}
	
	@Test
	def 'importRepoCLI success flow' () {
		given:
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		1 * projectManagementService.getProject(_, _ ) >> project
	
		when:
		def result = underTest.importRepoCLI('', 'DigitalBanking', 'DigitalBanking', 'https://dev.azure.com/ZionsETO/DTS/_git/zions-service-framework', 'v072160', 'K@nakadurga@2171')
		
		
		
		then:
		thrown(NullPointerException)
	
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
		return new CodeManagementService() 
	}
	
	@Bean
	ProjectManagementService projectManagementService() {
		return mockFactory.Mock(ProjectManagementService)
	}
	
	@Bean
	EndpointManagementService endpointManagementService() {
		return mockFactory.Mock(EndpointManagementService)
	}
	
	@Bean
	MemberManagementService memberManagementService() {
		return mockFactory.Mock(MemberManagementService)
	}
	
	@Bean
	PermissionsManagementService permissionsManagementService() {
		return mockFactory.Mock(PermissionsManagementService)
	}
	
}