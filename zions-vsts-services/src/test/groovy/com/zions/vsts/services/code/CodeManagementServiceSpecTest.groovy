package com.zions.vsts.services.code

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
	
	@Autowired
	private CommandManagementService commandManagementService
	
	
	def 'getRepos success flow with two params' () {
		
		given: 'stub rest call for to get repos'
		String json = this.getClass().getResource('/testdata/repos.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		//1 * genericRestClient.get(_) >> project
		
		when: 'call getRepos'
		def result = underTest.getRepos("eto-dev", project)
		
		then: 'result.count == 4'
		"${result.count}" == "4"
	}
	
	
	def 'getRepos success flow with three parms' () {
		given: 'Stub rest call for GIT repositories'
		
		String json = this.getClass().getResource('/testdata/repos.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		//1 * genericRestClient.get(_) >> project
		
		def team = new JsonSlurper().parseText(this.getClass().getResource('/testdata/projectteam.json').text)
		//1 * genericRestClient.get(_) >> team
		
		when: 'Access repos for a team'
		def result = underTest.getRepos("eto-dev", project, team)
		
		then: 'Result is zero'
		"${result.size}" == "0"
	}
	
	
	def 'listTopLevel success flow' () {
		given: 'Stub rest call for top level repositories'
		String json = this.getClass().getResource('/testdata/items.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		def repos = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repos.json').text)
		
		when: 'Call to get list of top level repos'
		def result = underTest.listTopLevel("eto-dev", project, repos)
		
		then: 'count == 1'
		"${result.count}" == "1"
		
	}
	
	
	def 'getFileContent success flow' () {
		given: 'Stub of rest call to get file content'
		String file = this.getClass().getResource('/testdata/dts-re-build.properties').text
		//JsonSlurper js = new JsonSlurper()
		//def out = js.parseText(json)
		//def out = content.file
		1 * genericRestClient.get(_) >> [content:file]
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		def repos = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repos.json').text)
		
		when: 'Call to get file content'
		def result = underTest.getFileContent("eto-dev", project, repos,'', "master")

		then: 'result != null'
		result != null
	}
	
	
	def 'getAuthUrl success flow' () {
		when: 'call getAuthUrl'
		def out = underTest.getAuthUrl('https://dev.azure.com', 'v072160', 'K@nakadurga@2171')
		
		then: 'out == "https://v072160:K%40nakadurga%402171@dev.azure.com"'
		out == "https://v072160:K%40nakadurga%402171@dev.azure.com"
	}
	
	/*
	def 'importRepoDir success flow' () {
		when: 
		def out = underTest.importRepoDir('', 'DigitalBanking', '', null, '', '')
		
		then:
		out != null
	}
	
	
	def 'importRepoCLI success flow' () {
		when: 
		def out = underTest.importRepoCLI('', 'DigitalBanking', '', '', '', '')
		
		then:
		out != null
	}*/
	
	
	def 'getRefHead success flow' () {
		
		given: 'stub of get repo refs call'
		String json = this.getClass().getResource('/testdata/refs.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		and: 'setup parameters'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		def repos = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repos.json').text)
		
		when: 'call getRefHead'
		def result = underTest.getRefHead('', project, repos)
		
		then: 'result.name == refs/heads/master'
		"${result.name}" == "refs/heads/master"
	}
	
	
	def 'createDeployManifest success flow' () {
		
		given: 'stub push rest call'
		String json = this.getClass().getResource('/testdata/pushes.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		and: 'setup parameters'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		def repos = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repos.json').text)
		
		when:  'call createDeployManifest'
		def result = underTest.createDeployManifest('', project, repos)
		
		then: 'No exception'
		result == null
	
	}

	
	def 'createRepo success flow' () {
		
		given: 'stub rest call for create repo'
		String json = this.getClass().getResource('/testdata/repos.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.post(_) >> out
		
		and: 'setup parameters'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when:  'call createRepo'
		def result = underTest.createRepo('',project,'DigitalBanking')
		
		then: 'result.count = 4'
		"${result.count}" == "4"
	
	}
	
	
	def 'getRepo success flow' () {
		
		given: 'stub of db rest call'
		String json = this.getClass().getResource('/testdata/DigitalBanking.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		and: 'setup parms'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when: 'call getRepo'
		def result = underTest.getRepo('',project,'DigitalBanking')
		
		then: 'result.id == 26862cc3-6775-4676-bb22-3bad625dcaa7'
		"${result.id}" == "26862cc3-6775-4676-bb22-3bad625dcaa7"
	
	}
	
	
	def 'getDeployManifest success flow' () {
		
		given: 'stub rest call for items'
		String json = this.getClass().getResource('/testdata/items.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		and: 'setup parameters'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		
		when:  'call getDeployManifest'
		def result = underTest.getDeployManifest('',project,repo)
		
		then: 'No exception'
		result ==null
	
	}
	
	
	def 'importRepo success flow' () {
		
		given: 'stub projectManagementService.getProject'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		1 * projectManagementService.getProject(_, _ ) >> project
		
		and: 'stub endpointManagementService.createServiceEndpoint'
		def endpoint = new JsonSlurper().parseText(this.getClass().getResource('/testdata/serviceendpoints.json').text)
		1 * endpointManagementService.createServiceEndpoint(_,_,_,_,_) >> endpoint
	
		and: 'stub rest call for import requests'
		String json = this.getClass().getResource('/testdata/importRequests.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		2 * genericRestClient.post(_) >> out
		
		when: 'importRepo'
		def result = underTest.importRepo('', 'DigitalBanking', 'DigitalBanking', '', '', '')
		
		then: 'result.count == 0'
		"${result.count}" =="0"
	
	}
	
	
	def 'importRepoDir NullPointerException flow' () {
		given: 'stub of projectManagementService.getProject'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		1 * projectManagementService.getProject(_, _ ) >> project
		when: 'call underTest.importRepoDir'
		def result = underTest.importRepoDir('', 'DigitalBanking', 'DigitialBanking', null, 'v072160', 'K@nakadurga@2171')
		
		then: 'thrown(NullPointerException)'
		thrown(NullPointerException)
	
	}
	
	
	def 'importRepoCLI NullPointerException flow' () {
		given: 'stub of projectManagementService.getProject'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		1 * projectManagementService.getProject(_, _ ) >> project
		
		when: 'call importRepoCLI'
		def result = underTest.importRepoCLI('', 'DigitalBanking', 'DigitialBanking', null, 'v072160', 'K@nakadurga@2171')
		
		then: 'thrown(NullPointerException)'
		thrown(NullPointerException)
	
	}
	
	/*
	def 'importRepoCLI success flow' () {
		given:
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		1 * projectManagementService.getProject(_, _ ) >> project
	
		when: 
		def result = underTest.importRepoCLI('', 'DigitalBanking', 'DigitalBanking', 'https://dev.azure.com/ZionsETO/DTS/g_it/zions-service-framework', 'v072160', 'K@nakadurga@2171')
		
		
		
		then:
		thrown(NullPointerException)
	
	}*/
	
	
	def 'ensureDeployManifest success flow' () {
		
		given: 'stub check for items'
		String json = this.getClass().getResource('/testdata/items.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		and: 'stub rest call for refs'
		String json1 = this.getClass().getResource('/testdata/refs.json').text
		JsonSlurper js1 = new JsonSlurper()
		def out1 = js1.parseText(json1)
		1 * genericRestClient.get(_) >> out1
		
		and: 'stub rest call for code pushes'
		String json2 = this.getClass().getResource('/testdata/codepushes.json').text
		JsonSlurper js2 = new JsonSlurper()
		def out2 = js2.parseText(json1)
		2 * genericRestClient.post(_) >> out2
		
		and: 'setup parameters'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		
		when:  'call ensureDeployManifest'
		def result = underTest.ensureDeployManifest('',project,repo)
		
		then: 'No exception'
		result != null
	
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
	
	@Bean
	CommandManagementService commandManagementService() {
		return mockFactory.Mock(CommandManagementService);
	}
}