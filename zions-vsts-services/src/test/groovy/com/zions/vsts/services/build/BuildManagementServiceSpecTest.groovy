package com.zions.vsts.services.build

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
@ContextConfiguration(classes=[BuildManagementServiceTestConfig])
class BuildManagementServiceSpecTest extends Specification {
	
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
	private CodeManagementService codeManagementService
	
	@Autowired
	private CommandManagementService commandManagementService
	
	@Autowired
	private BuildManagementService underTest
	
	@Test
	def 'getRepos success flow' () {
	
		when:
		def result = underTest.getResource('gradle','CI')
		
		then:
		"${result.id}" == "83"
	}
	
	@Test
	def 'getTemplate success flow' () {
		given:
		String json = this.getClass().getResource('/testdata/buildtemplates.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when:
		def result = underTest.getTemplate('', project, 'DigitalBanking')
		
		then:
		result == null
	}
	
	@Test
	def 'writeBuildDefinition success flow' () {
		given:
		String json = this.getClass().getResource('/testdata/builddefinitions.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.post(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when:
		def result = underTest.writeBuildDefinition('', project, '')
		
		then:
		"${result.count}" == "1"
	}
	
	@Test
	def 'getQueue success flow' () {
		given:
		String json = this.getClass().getResource('/testdata/buildqueues.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when:
		def result = underTest.getQueue('', project, '')
		
		then:
		result == null
	}
	
	@Test
	def 'getRetentionSettings success flow' () {
		given:
		String json = this.getClass().getResource('/testdata/buildsettings.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		when:
		def result = underTest.getRetentionSettings('')
		
		then:
		"${result.daysToKeepDeletedBuildsBeforeDestroy}" =="30"
	}
	
	/*@Test
	def 'getBuild success flow' () {
		given:
		String json = this.getClass().getResource('/testdata/builddefinitions.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		def repos = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repos.json').text)
		
		when:
		def result = underTest.getBuild('', project, '','')
		
		then:
		"${result.count}" =="1"
	}*/
	
	@Test
	def 'createBuildFolder success flow' () {
		given:
		String json = this.getClass().getResource('/testdata/buildfolders.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.put(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		def repos = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repos.json').text)
		
		when:
		def result = underTest.createBuildFolder('', project, '')
		
		then:
		"${result.count}" =="1"
	}
	
	@Test
	def 'getBuild success flow' () {
		given:
		String json = this.getClass().getResource('/testdata/builddefinitions.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		and:
		String json1 = this.getClass().getResource('/testdata/builddefinations35.json').text
		JsonSlurper js1 = new JsonSlurper()
		def out1 = js1.parseText(json1)
		1 * genericRestClient.get(_) >> out1
		
		and:
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when:
		def result = underTest.getBuild('', project, 'DigitalBanking')
		
		then:
		"${result.id}" =="35"
	}
	
	@Test
	def 'getDRBuild success flow' () {
		given:
		String json = this.getClass().getResource('/testdata/builddefinitions.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		and:
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		
		when:
		def result = underTest.getDRBuild('', project, repo,'')
		
		then:
		"${result.count}" =="1"
	}
	
	@Test
	def 'getBuild success flow with 4 parms' () {
		given:
		String json = this.getClass().getResource('/testdata/builddefinitions.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		and:
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		
		when:
		def result = underTest.getBuild('', project, repo,'')
		
		then:
		"${result.count}" =="1"
	}
	
	@Test
	def 'createBuildDefinition success flow' () {
		given:
		String json = this.getClass().getResource('/testdata/buildConfigurations.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		and:
		String json1 = this.getClass().getResource('/testdata/buildqueues.json').text
		JsonSlurper js1 = new JsonSlurper()
		def out1 = js.parseText(json1)
		1 * genericRestClient.get(_) >> out1
		
		and:
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		def bDef = new JsonSlurper().parseText(this.getClass().getResource('/testdata/bdef.json').text)
		
		when:
		def result = underTest.createBuildDefinition('', project, repo, bDef, 'Dev', '')
		
		then:
		result == null
	}
	
	@Test
	def 'createDRBuildDefinition success flow' () {
		given:
		String json = this.getClass().getResource('/testdata/buildConfigurations.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		and:
		String json1 = this.getClass().getResource('/testdata/buildqueues.json').text
		JsonSlurper js1 = new JsonSlurper()
		def out1 = js.parseText(json1)
		1 * genericRestClient.get(_) >> out1
		
		and:
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		def bDef = new JsonSlurper().parseText(this.getClass().getResource('/testdata/bdef.json').text)
		
		when:
		def result = underTest.createDRBuildDefinition('', project, repo, bDef, 'Dev', '') 
		
		then:
		result == null
	}
	
	@Test
	def 'createBuild success flow' () {
		given:
		String json = this.getClass().getResource('/testdata/buildConfigurations.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		and:
		String json1 = this.getClass().getResource('/testdata/buildqueues.json').text
		JsonSlurper js1 = new JsonSlurper()
		def out1 = js.parseText(json1)
		1 * genericRestClient.get(_) >> out1
		
		and:
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		//def bDef = new JsonSlurper().parseText(this.getClass().getResource('/testdata/bdef.json').text)
		
		when:
		def result = underTest.createBuild('', project, repo, BuildType.GRADLE, 'Dev', '') 
		
		then:
		result == null
	}
	
	@Test
	def 'branchPolicy success flow' () {
		given:
		String json = this.getClass().getResource('/testdata/buildConfigurations.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.post(_) >> out
				
		and:
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		def ciBuild = new JsonSlurper().parseText(this.getClass().getResource('/testdata/cibuild.json').text)
		def branch = new JsonSlurper().parseText(this.getClass().getResource('/testdata/branch.json').text)
		
		when:
		def result = underTest.branchPolicy('', project, repo, ciBuild, branch)
		
		then:
		"${result.count}" == "0"
	}
	
	@Test
	def 'getBuildTemplate success flow' () {
		given:
		/*String json = this.getClass().getResource('/testdata/buildConfigurations.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.post(_) >> out
				
		and:*/
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		
		when:
		def result = underTest.getBuildTemplate('', project, repo, 'Dev')
		
		then:
		result == null
	}
	
		
}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class BuildManagementServiceTestConfig {
	def mockFactory = new DetachedMockFactory()
	
	@Bean
	IGenericRestClient genericRestClient() {
		return mockFactory.Mock(GenericRestClient, name: 'genericRestClient')
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
	CommandManagementService commandManagementService() {
		return mockFactory.Mock(CommandManagementService);
	}
	
	@Bean
	BuildManagementService underTest() {
		return new BuildManagementService();
	}
	
}