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
	
	@Test
	def 'getResource success flow' () {
	
		when: w_ 'Test getting resource'
		def result = underTest.getResource('gradle','CI',null)
		
		then: t_ 'Resource id is 83'
		"${result.id}" == "83"
	}
	
	@Test
	def 'getTemplate success flow' () {
		given: g_ 'Stub rest call for get build templates'
		String json = this.getClass().getResource('/testdata/buildtemplates.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when: w_ 'Run get template call'
		def result = underTest.getTemplate('', project, 'Azure Cloud Services')
		
		then: t_ 'result not equal to null'
		result != null
	}
	
	@Test
	def 'writeBuildDefinition success flow' () {
		given: g_ 'Stub rest call'
		String json = this.getClass().getResource('/testdata/builddefinitions.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.post(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when: w_ 'Write build definition.'
		def result = underTest.writeBuildDefinition('', project, '')
		
		then: t_ 'Result has count of 1'
		"${result.count}" == "1"
	}
	
	@Test
	def 'getQueue success flow' () {
		given: g_ 'Stub rest call to get build queues'
		String json = this.getClass().getResource('/testdata/buildqueues.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when: w_ 'Call get build queue'
		def result = underTest.getQueue('', project, '')
		
		then: t_ 'Result is null'
		result == null
	}
	
	@Test
	def 'getRetentionSettings success flow' () {
		given: g_ 'Sutb call for getting build settings'
		String json = this.getClass().getResource('/testdata/buildsettings.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		when: w_ 'call getRetentionSettings'
		def result = underTest.getRetentionSettings('')
		
		then: t_ 'result.daysToKeepDeletedBuildsBeforeDestroy == 30'
		"${result.daysToKeepDeletedBuildsBeforeDestroy}" =="30"
	}
	
	/*@Test
	def 'getBuild success flow' () {
		given: g_
		String json = this.getClass().getResource('/testdata/builddefinitions.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		def repos = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repos.json').text)
		
		when: w_
		def result = underTest.getBuild('', project, '','')
		
		then: t_
		"${result.count}" =="1"
	}*/
	
	@Test
	def 'createBuildFolder success flow' () {
		given: g_ 'stub rest call for build folders'
		String json = this.getClass().getResource('/testdata/buildfolders.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.put(_) >> out
		
		and: a_ "setup parameters for call"
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		def repos = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repos.json').text)
		
		when: w_ 'call createBuildFolder'
		def result = underTest.createBuildFolder('', project, '')
		
		then: t_ 'result.count == 1'
		"${result.count}" =="1"
	}
	
	@Test
	def 'getBuild success flow' () {
		given: g_ 'stub build def rest call'
		String json = this.getClass().getResource('/testdata/builddefinitions.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		and: a_ 'stub specific build def call'
		String json1 = this.getClass().getResource('/testdata/builddefinations35.json').text
		JsonSlurper js1 = new JsonSlurper()
		def out1 = js1.parseText(json1)
		1 * genericRestClient.get(_) >> out1
		
		and: a_ 'setup parameters'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when: w_ 'call getBuild'
		def result = underTest.getBuild('', project, 'DigitalBanking')
		
		then: t_ 'result.id == 35'
		"${result.id}" =="35"
	}
	
	@Test
	def 'getDRBuild success flow' () {
		given: g_ 'stub get build defs rest call'
		String json = this.getClass().getResource('/testdata/builddefinitions.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		and: a_ 'setup parameters'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		
		when: w_ 'call getDRBuild'
		def result = underTest.getDRBuild('', project, repo,'')
		
		then: t_ 'result.count == 1'
		"${result.count}" =="1"
	}
	
	@Test
	def 'getBuild success flow with 4 parms' () {
		given: g_ 'stub rest call for build defs'
		String json = this.getClass().getResource('/testdata/builddefinitions.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		and: a_ 'setup parameters'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		
		when: w_ 'call getBuild'
		def result = underTest.getBuild('', project, repo,'')
		
		then: t_ 'result.count == 1'
		"${result.count}" =="1"
	}
	
	@Test
	def 'createBuildDefinition success flow' () {
		given: g_ 'stub get build config call'
		String json = this.getClass().getResource('/testdata/buildConfigurations.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		// the call to getQueue only happens if class variable useTfsTemplate is false, but not sure how to 
		// change the value of this given it is autowired
		//and: a_
		//String json1 = this.getClass().getResource('/testdata/buildqueues.json').text
		//JsonSlurper js1 = new JsonSlurper()
		//def out1 = js.parseText(json1)
		//1 * genericRestClient.get(_) >> out1
		
		and: a_ 'setup data for createBuildDefinition parameters'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		def bDef = new JsonSlurper().parseText(this.getClass().getResource('/testdata/bdef.json').text)
		
		when: w_ 'call createBuildDefinition'
		def result = underTest.createBuildDefinition('', project, repo, bDef, 'Dev', '')
		
		then: t_ null
		result == null
	}
	
	@Test
	def 'createDRBuildDefinition success flow' () {
		given: g_ 'stub rest call for getting build configurations'
		String json = this.getClass().getResource('/testdata/buildConfigurations.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		and: a_ 'stub call for getting build queues'
		String json1 = this.getClass().getResource('/testdata/buildqueues.json').text
		JsonSlurper js1 = new JsonSlurper()
		def out1 = js.parseText(json1)
		1 * genericRestClient.get(_) >> out1
		
		and: a_ 'setup data for test parameters'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		def bDef = new JsonSlurper().parseText(this.getClass().getResource('/testdata/bdef.json').text)
		
		when: w_ 'call createDRBuildDefinition'
		def result = underTest.createDRBuildDefinition('', project, repo, bDef, 'release', '') 
		
		then: t_ null
		result == null
	}
	
	@Test
	def 'createBuild success flow' () {
		given: g_ 'stub rest cal for build configs'
		String json = this.getClass().getResource('/testdata/buildConfigurations.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		and: a_ 'stub rest call for build queues'
		String json1 = this.getClass().getResource('/testdata/buildqueues.json').text
		JsonSlurper js1 = new JsonSlurper()
		def out1 = js.parseText(json1)
		1 * genericRestClient.get(_) >> out1
		
		and: a_ 'setup parameters for createBuild '
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		//def bDef = new JsonSlurper().parseText(this.getClass().getResource('/testdata/bdef.json').text)
		
		when: w_ 'call createBuild'
		def result = underTest.createBuild('', project, repo, BuildType.GRADLE, 'Dev', '') 
		
		then: t_ null
		result == null
	}
	
	@Test
	def 'branchPolicy success flow' () {
		given: g_ 'stub rest call'
		String json = this.getClass().getResource('/testdata/buildConfigurations.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.post(_) >> out
				
		and: a_ 'setup parameters'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		def ciBuild = new JsonSlurper().parseText(this.getClass().getResource('/testdata/cibuild.json').text)
		def branch = new JsonSlurper().parseText(this.getClass().getResource('/testdata/branch.json').text)
		
		when: w_ 'call branchPolicy'
		def result = underTest.branchPolicy('', project, repo, ciBuild, branch)
		
		then: t_ 'result.count == 0'
		"${result.count}" == "0"
	}
	
	@Test 
	def 'getBuildTemplate success flow' () {
		given: g_ 'data for template call'
		def items = "build-template-gradle = gradle"//new JsonSlurper().parseText(this.getClass().getResource('/testdata/items.json').text)
		
		and: a_ 'stub of get build templates rest call'
		String json = this.getClass().getResource('/testdata/buildtemplates.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		2 * genericRestClient.get(_) >> out
		
		and: a_ 'setup of parameters'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		
		when: w_ 'call getBuildTemplate'
		def result = underTest.getBuildTemplate('', project, repo, 'ci', 'build-template-gradle')
		
		then: t_ null
		result == null
	}
	
	@Test
	def 'reviseReleaseLabels success flow' () {
		given: g_ 'mock codeManagementService'
		def repos = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repos.json').text)
		1 * codeManagementService.getRepos(_,_) >> repos
				
		and: a_ 'setup parameters'
		def projectData = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when: w_ 'call reviseReleaseLabels'
		def result = underTest.reviseReleaseLabels('', projectData, 'MobileBanking', '')
		
		then: t_ null
		result != null
	}
	
	@Test
	def 'ensureDRBuilds success flow' () {
		given: g_ 'stub of rest call for build defs'
		String json = this.getClass().getResource('/testdata/builddefinitions.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		2 * genericRestClient.get(_) >> out
				
		and: a_ 'setup call parameters'
		def projectData = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		
		when: w_ 'call ensureDRBuilds'
		def result = underTest.ensureDRBuilds('', projectData, repo)
		
		then: t_ 'result.folderName == DigitalBanking'
		"${result.folderName}" == "DigitalBanking"
	}
	
	@Test
	def 'ensureDRBuilds success with build count zero flow' () {
		given: g_ 'stub rest calls for build defs'
		String json = this.getClass().getResource('/testdata/builddefinitionscountzero.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		4 * genericRestClient.get(_) >> out
				
		and: a_ 'setup parameters'
		def projectData = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		
		when: w_ 'call ensureDRBuilds'
		def result = underTest.ensureDRBuilds('', projectData, repo)
		
		then: t_ 'result.folderName == DigitalBanking'
		"${result.folderName}" == "DigitalBanking"
	}
	
	@Test
	def 'ensureBuildsForBranch success flow' () {
		given: g_ 'stub for rest call to get build defs'
		String json = this.getClass().getResource('/testdata/builddefinitions.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		2 * genericRestClient.get(_) >> out
				
		and: a_ 'setup parameters'
		def projectData = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		
		when: w_ 'call ensureBuildsForBranch'
		def result = underTest.ensureBuildsForBranch('', projectData, repo, false, null, null)
		
		then: t_ "resultFolderName is empty"
		"${result.folderName}" == ""
	}
	
	@Test
	def 'ensureBuildsForBranch success with build count zero flow' () {
		given: g_ 'stub rest call for build definitions with zero count'
		String json = this.getClass().getResource('/testdata/builddefinitionscountzero.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		genericRestClient.get(_) >> out
				
		and: a_ 'setup parameters'
		def projectData = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		
		when: w_ 'call ensureBuildsForBranch'
		def result = underTest.ensureBuildsForBranch('', projectData, repo, false, null, null)
		
		then: t_ 'result.folderName is empty'
		"${result.folderName}" == ""
	}
	
	@Test
	def 'ensureBuilds success flow' () {
		given: g_ 'stub projectManagementService.getProject'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		1 * projectManagementService.getProject(_,_,true) >> project
		
		and: a_ 'stub codeManagementService.getRepos'
		def repos = new JsonSlurper().parseText(this.getClass().getResource('/testdata/testrepos.json').text)
		1 * codeManagementService.getRepos(_,_,_) >> repos
		
		and: a_ 'mock codeManagementService calls'
		def items = new JsonSlurper().parseText(this.getClass().getResource('/testdata/items.json').text)
		4 * codeManagementService.listTopLevel(_,_,_) >> items
		4 * codeManagementService.ensureDeployManifest(_,_,_) >> items
		
		def team = new JsonSlurper().parseText(this.getClass().getResource('/testdata/projectteam.json').text)

		def folder =""
	
		and: a_ 'stub rest call for build defs'
		String json = this.getClass().getResource('/testdata/builddefinitions.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		8 * genericRestClient.get(_) >> out
	
		when: w_ 'call ensureBuilds'
		def result = underTest.ensureBuilds('', 'project', folder, team)
		
		then: t_ 'result.size() == 4'
		result.size() == 4
	}
	
	@Test
	def 'detectBuildType gradle success flow' () {
		given: g_ 'stub code management service calls'
		def items = new JsonSlurper().parseText(this.getClass().getResource('/testdata/items.json').text)
		1 * codeManagementService.listTopLevel(_,_,_) >> items
		
		def pomitems = new JsonSlurper().parseText(this.getClass().getResource('/testdata/pomitems.json').text)
		1 * codeManagementService.listTopLevel(_,_,_) >> pomitems
		
		def nodeitems = new JsonSlurper().parseText(this.getClass().getResource('/testdata/nodeitems.json').text)
		1 * codeManagementService.listTopLevel(_,_,_) >> nodeitems
		
		def antitems = new JsonSlurper().parseText(this.getClass().getResource('/testdata/antitems.json').text)
		1 * codeManagementService.listTopLevel(_,_,_) >> antitems
		
		and: a_ 'setup parameter data'
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repos.json').text)
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		
		when: w_ 'call detectBuildType'
		def resultGradle = underTest.detectBuildType('', project, repo)
		def resultMaven = underTest.detectBuildType('', project, repo)
		def resultNode = underTest.detectBuildType('', project, repo)
		def resultAnt = underTest.detectBuildType('', project, repo)
		
		then: t_ null
		"GRADLE" == resultGradle.toString()
		"MAVEN" == resultMaven.toString()
		"NODE" == resultNode.toString()
		"ANT" == resultAnt.toString()
	}
	
	@Test
	def 'ensureBuild successflow' () {
		given: g_ 'stub get build defs'
		String json = this.getClass().getResource('/testdata/builddefinitionscountzero.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		and: a_ 'stub get build configs'
		String json1 = this.getClass().getResource('/testdata/buildConfigurations.json').text
		JsonSlurper js1 = new JsonSlurper()
		def out1 = js.parseText(json1)
		1 * genericRestClient.get(_) >> out1
		
		and: a_ 'stub get build queues'
		String json2 = this.getClass().getResource('/testdata/buildqueues.json').text
		JsonSlurper js2 = new JsonSlurper()
		def out2 = js.parseText(json2)
		1 * genericRestClient.get(_) >> out2
		
		and: a_ 'setup parameters'
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repos.json').text)
		def folder =""
		def buildStage = ""
		
		when: w_ 'call ensureBuild'
		def result = underTest.ensureBuild('', project, repo, BuildType.GRADLE, '', folder)
		
		then: t_ null
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