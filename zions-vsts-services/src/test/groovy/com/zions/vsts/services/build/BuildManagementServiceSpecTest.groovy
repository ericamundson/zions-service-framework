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
 * Variety of test case for unit testing BuildManagementService
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
	
	
	def 'getResource success flow' () {
	
		when: 'Test getting resource'
		def result = underTest.getResource('gradle','CI',null)
		
		then: 'Resource id is 83'
		"${result.id}" == "83"
	}
	
	
	def 'getTemplate success flow' () {
		given: 'Stub rest call for get build templates'
		String json = this.getClass().getResource('/testdata/buildtemplates.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when: 'Run get template call'
		def result = underTest.getTemplate('', project, 'Azure Cloud Services')
		
		then: 'result not equal to null'
		result != null
	}
	
	
	def 'writeBuildDefinition success flow' () {
		given: 'Stub rest call'
		String json = this.getClass().getResource('/testdata/builddefinitions.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.post(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when: 'Write build definition.'
		def result = underTest.writeBuildDefinition('', project, '')
		
		then: 'Result has count of 1'
		"${result.count}" == "1"
	}
	
	
	def 'getQueue success flow' () {
		given: 'Stub rest call to get build queues'
		String json = this.getClass().getResource('/testdata/buildqueues.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when: 'Call get build queue'
		def result = underTest.getQueue('', project, '')
		
		then: 'Result is null'
		result == null
	}
	
	
	def 'getRetentionSettings success flow' () {
		given: 'Sutb call for getting build settings'
		String json = this.getClass().getResource('/testdata/buildsettings.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		when: 'call getRetentionSettings'
		def result = underTest.getRetentionSettings('')
		
		then: 'result.daysToKeepDeletedBuildsBeforeDestroy == 30'
		"${result.daysToKeepDeletedBuildsBeforeDestroy}" =="30"
	}
	
	/*
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
	
	
	def 'createBuildFolder success flow' () {
		given: 'stub rest call for build folders'
		String json = this.getClass().getResource('/testdata/buildfolders.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.put(_) >> out
		
		and: "setup parameters for call"
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		def repos = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repos.json').text)
		
		when: 'call createBuildFolder'
		def result = underTest.createBuildFolder('', project, '')
		
		then: 'result.count == 1'
		"${result.count}" =="1"
	}
	
	
	def 'getBuild success flow' () {
		given: 'stub build def rest call'
		String json = this.getClass().getResource('/testdata/builddefinitions.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		and: 'stub specific build def call'
		String json1 = this.getClass().getResource('/testdata/builddefinations35.json').text
		JsonSlurper js1 = new JsonSlurper()
		def out1 = js1.parseText(json1)
		1 * genericRestClient.get(_) >> out1
		
		and: 'setup parameters'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when: 'call getBuild'
		def result = underTest.getBuild('', project, 'DigitalBanking')
		
		then: 'result.id == 35'
		"${result.id}" =="35"
	}
	
	
	def 'getDRBuild success flow' () {
		given: 'stub get build defs rest call'
		String json = this.getClass().getResource('/testdata/builddefinitions.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		and: 'setup parameters'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		
		when: 'call getDRBuild'
		def result = underTest.getDRBuild('', project, repo,'')
		
		then: 'result.count == 1'
		"${result.count}" =="1"
	}
	
	
	def 'getBuild success flow with 4 parms' () {
		given: 'stub rest call for build defs'
		String json = this.getClass().getResource('/testdata/builddefinitions.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		and: 'setup parameters'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		
		when: 'call getBuild'
		def result = underTest.getBuild('', project, repo,'')
		
		then: 'result.count == 1'
		"${result.count}" =="1"
	}
	
	
	def 'createBuildDefinition success flow' () {
		given: 'stub get build config call'
		String json = this.getClass().getResource('/testdata/buildConfigurations.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		// the call to getQueue only happens if class variable useTfsTemplate is false, but not sure how to 
		// change the value of this given it is autowired
		//and:
		//String json1 = this.getClass().getResource('/testdata/buildqueues.json').text
		//JsonSlurper js1 = new JsonSlurper()
		//def out1 = js.parseText(json1)
		//1 * genericRestClient.get(_) >> out1
		
		and: 'setup data for createBuildDefinition parameters'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		def bDef = new JsonSlurper().parseText(this.getClass().getResource('/testdata/bdef.json').text)
		
		when: 'call createBuildDefinition'
		def result = underTest.createBuildDefinition('', project, repo, bDef, 'Dev', '')
		
		then: 'No exception'
		result == null
	}
	
	
	def 'createDRBuildDefinition success flow' () {
		given: 'stub rest call for getting build configurations'
		String json = this.getClass().getResource('/testdata/buildConfigurations.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		and: 'stub call for getting build queues'
		String json1 = this.getClass().getResource('/testdata/buildqueues.json').text
		JsonSlurper js1 = new JsonSlurper()
		def out1 = js.parseText(json1)
		1 * genericRestClient.get(_) >> out1
		
		and: 'setup data for test parameters'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		def bDef = new JsonSlurper().parseText(this.getClass().getResource('/testdata/bdef.json').text)
		
		when: 'call createDRBuildDefinition'
		def result = underTest.createDRBuildDefinition('', project, repo, bDef, 'release', '') 
		
		then: 'No exception'
		result == null
	}
	
	
	def 'createBuild success flow' () {
		given: 'stub rest cal for build configs'
		String json = this.getClass().getResource('/testdata/buildConfigurations.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		and: 'stub rest call for build queues'
		String json1 = this.getClass().getResource('/testdata/buildqueues.json').text
		JsonSlurper js1 = new JsonSlurper()
		def out1 = js.parseText(json1)
		1 * genericRestClient.get(_) >> out1
		
		and: 'setup parameters for createBuild '
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		//def bDef = new JsonSlurper().parseText(this.getClass().getResource('/testdata/bdef.json').text)
		
		when: 'call createBuild'
		def result = underTest.createBuild('', project, repo, BuildType.GRADLE, 'Dev', '') 
		
		then: 'No exception'
		result == null
	}
	
	
	def 'branchPolicy success flow' () {
		given: 'stub rest call'
		String json = this.getClass().getResource('/testdata/buildConfigurations.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.post(_) >> out
				
		and: 'setup parameters'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		def ciBuild = new JsonSlurper().parseText(this.getClass().getResource('/testdata/cibuild.json').text)
		def branch = new JsonSlurper().parseText(this.getClass().getResource('/testdata/branch.json').text)
		
		when: 'call branchPolicy'
		def result = underTest.branchPolicy('', project, repo, ciBuild, branch)
		
		then: 'result.count == 0'
		"${result.count}" == "0"
	}
	
	 
	def 'getBuildTemplate success flow' () {
		given: 'data for template call'
		def items = "build-template-gradle = gradle"//new JsonSlurper().parseText(this.getClass().getResource('/testdata/items.json').text)
		
		and: 'stub of get build templates rest call'
		String json = this.getClass().getResource('/testdata/buildtemplates.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		2 * genericRestClient.get(_) >> out
		
		and: 'setup of parameters'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		
		when: 'call getBuildTemplate'
		def result = underTest.getBuildTemplate('', project, repo, 'ci', 'build-template-gradle')
		
		then: 'No exception'
		result == null
	}
	
	
	def 'reviseReleaseLabels success flow' () {
		given: 'mock codeManagementService'
		def repos = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repos.json').text)
		1 * codeManagementService.getRepos(_,_) >> repos
				
		and: 'setup parameters'
		def projectData = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when: 'call reviseReleaseLabels'
		def result = underTest.reviseReleaseLabels('', projectData, 'MobileBanking', '')
		
		then: 'No exception'
		result != null
	}
	
	
	def 'ensureDRBuilds success flow' () {
		given: 'stub of rest call for build defs'
		String json = this.getClass().getResource('/testdata/builddefinitions.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		2 * genericRestClient.get(_) >> out
				
		and: 'setup call parameters'
		def projectData = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		
		when: 'call ensureDRBuilds'
		def result = underTest.ensureDRBuilds('', projectData, repo)
		
		then: 'result.folderName == DigitalBanking'
		"${result.folderName}" == "DigitalBanking"
	}
	
	
	def 'ensureDRBuilds success with build count zero flow' () {
		given: 'stub rest calls for build defs'
		String json = this.getClass().getResource('/testdata/builddefinitionscountzero.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		4 * genericRestClient.get(_) >> out
				
		and: 'setup parameters'
		def projectData = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		
		when: 'call ensureDRBuilds'
		def result = underTest.ensureDRBuilds('', projectData, repo)
		
		then: 'result.folderName == DigitalBanking'
		"${result.folderName}" == "DigitalBanking"
	}
	
	
	def 'ensureBuildsForBranch success flow' () {
		given: 'stub for rest call to get build defs'
		String json = this.getClass().getResource('/testdata/builddefinitions.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		2 * genericRestClient.get(_) >> out
				
		and: 'setup parameters'
		def projectData = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		
		when: 'call ensureBuildsForBranch'
		def result = underTest.ensureBuildsForBranch('', projectData, repo, null, null, false)
		
		then: "resultFolderName is empty"
		"${result.folderName}" == ""
	}
	
	
	def 'ensureBuildsForBranch success with build count zero flow' () {
		given: 'stub rest call for build definitions with zero count'
		String json = this.getClass().getResource('/testdata/builddefinitionscountzero.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		genericRestClient.get(_) >> out
				
		and: 'setup parameters'
		def projectData = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		
		when: 'call ensureBuildsForBranch'
		def result = underTest.ensureBuildsForBranch('', projectData, repo, null, null, false)
		
		then: 'result.folderName is empty'
		"${result.folderName}" == ""
	}
	
	
	def 'ensureBuilds success flow' () {
		given: 'stub projectManagementService.getProject'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		1 * projectManagementService.getProject(_,_,true) >> project
		
		and: 'stub codeManagementService.getRepos'
		def repos = new JsonSlurper().parseText(this.getClass().getResource('/testdata/testrepos.json').text)
		1 * codeManagementService.getRepos(_,_,_) >> repos
		
		and: 'mock codeManagementService calls'
		def items = new JsonSlurper().parseText(this.getClass().getResource('/testdata/items.json').text)
		4 * codeManagementService.listTopLevel(_,_,_) >> items
		4 * codeManagementService.ensureDeployManifest(_,_,_) >> items
		
		def team = new JsonSlurper().parseText(this.getClass().getResource('/testdata/projectteam.json').text)

		def folder =""
	
		and: 'stub rest call for build defs'
		String json = this.getClass().getResource('/testdata/builddefinitions.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		8 * genericRestClient.get(_) >> out
	
		when: 'call ensureBuilds'
		def result = underTest.ensureBuilds('', 'project', folder, team)
		
		then: 'result.size() == 4'
		result.size() == 4
	}
	
	
	def 'detectBuildType gradle success flow' () {
		given: 'stub code management service calls'
		def items = new JsonSlurper().parseText(this.getClass().getResource('/testdata/items.json').text)
		1 * codeManagementService.listTopLevel(_,_,_) >> items
		
		def pomitems = new JsonSlurper().parseText(this.getClass().getResource('/testdata/pomitems.json').text)
		1 * codeManagementService.listTopLevel(_,_,_) >> pomitems
		
		def nodeitems = new JsonSlurper().parseText(this.getClass().getResource('/testdata/nodeitems.json').text)
		1 * codeManagementService.listTopLevel(_,_,_) >> nodeitems
		
		def antitems = new JsonSlurper().parseText(this.getClass().getResource('/testdata/antitems.json').text)
		1 * codeManagementService.listTopLevel(_,_,_) >> antitems
		
		and: 'setup parameter data'
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repos.json').text)
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		
		when: 'call detectBuildType'
		def resultGradle = underTest.detectBuildType('', project, repo)
		def resultMaven = underTest.detectBuildType('', project, repo)
		def resultNode = underTest.detectBuildType('', project, repo)
		def resultAnt = underTest.detectBuildType('', project, repo)
		
		then: 'No exception'
		"GRADLE" == resultGradle.toString()
		"MAVEN" == resultMaven.toString()
		"NODE" == resultNode.toString()
		"ANT" == resultAnt.toString()
	}
	
	
	def 'ensureBuild successflow' () {
		given: 'stub get build defs'
		String json = this.getClass().getResource('/testdata/builddefinitionscountzero.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		and: 'stub get build configs'
		String json1 = this.getClass().getResource('/testdata/buildConfigurations.json').text
		JsonSlurper js1 = new JsonSlurper()
		def out1 = js.parseText(json1)
		1 * genericRestClient.get(_) >> out1
		
		and: 'stub get build queues'
		String json2 = this.getClass().getResource('/testdata/buildqueues.json').text
		JsonSlurper js2 = new JsonSlurper()
		def out2 = js.parseText(json2)
		1 * genericRestClient.get(_) >> out2
		
		and: 'setup parameters'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repos.json').text)
		def folder =""
		def buildStage = ""
		
		when: 'call ensureBuild'
		def result = underTest.ensureBuild('', project, repo, BuildType.GRADLE, '', folder)
		
		then: 'No exception'
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