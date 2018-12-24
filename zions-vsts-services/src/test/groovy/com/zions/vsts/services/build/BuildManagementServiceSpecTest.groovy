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
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		
		when:
		def result = underTest.getBuildTemplate('', project, repo, 'Dev')
		
		then:
		result == null
	}
	
	@Test
	def 'getBuildTemplate other success flow' () {
		given:
		def items = "build-template-gradle = gradle"//new JsonSlurper().parseText(this.getClass().getResource('/testdata/items.json').text)
		1 * codeManagementService.getBuildPropertiesFile(_,_,_,_) >> items
		
		and:
		String json = this.getClass().getResource('/testdata/buildtemplates.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		2 * genericRestClient.get(_) >> out
		
		and:
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		
		when:
		def result = underTest.getBuildTemplate('', project, repo, 'gradle')
		
		then:
		result == null
	}
	
	@Test
	def 'reviseReleaseLabels success flow' () {
		given:
		def repos = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repos.json').text)
		1 * codeManagementService.getRepos(_,_) >> repos
				
		and:
		def projectData = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		when:
		def result = underTest.reviseReleaseLabels('', projectData, 'MobileBanking', '')
		
		then:
		result != null
	}
	
	@Test
	def 'ensureDRBuilds success flow' () {
		given:
		String json = this.getClass().getResource('/testdata/builddefinitions.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		2 * genericRestClient.get(_) >> out
				
		and:
		def projectData = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		
		when:
		def result = underTest.ensureDRBuilds('', projectData, repo)
		
		then:
		"${result.folderName}" == "DigitalBanking"
	}
	
	@Test
	def 'ensureDRBuilds success with build count zero flow' () {
		given:
		String json = this.getClass().getResource('/testdata/builddefinitionscountzero.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		4 * genericRestClient.get(_) >> out
				
		and:
		def projectData = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		
		when:
		def result = underTest.ensureDRBuilds('', projectData, repo)
		
		then:
		"${result.folderName}" == "DigitalBanking"
	}
	
	@Test
	def 'ensureBuildsForBranch success flow' () {
		given:
		String json = this.getClass().getResource('/testdata/builddefinitions.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		2 * genericRestClient.get(_) >> out
				
		and:
		def projectData = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		
		when:
		def result = underTest.ensureBuildsForBranch('', projectData, repo, false)
		
		then:
		"${result.folderName}" == ""
	}
	
	@Test
	def 'ensureBuildsForBranch success with build count zero flow' () {
		given:
		String json = this.getClass().getResource('/testdata/builddefinitionscountzero.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		2 * genericRestClient.get(_) >> out
				
		and:
		def projectData = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repo.json').text)
		
		when:
		def result = underTest.ensureBuildsForBranch('', projectData, repo, false)
		
		then:
		"${result.folderName}" == ""
	}
	
	@Test
	def 'ensureBuilds success flow' () {
		given:
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		1 * projectManagementService.getProject(_,_,true) >> project
		
		and: 
		def repos = new JsonSlurper().parseText(this.getClass().getResource('/testdata/testrepos.json').text)
		1 * codeManagementService.getRepos(_,_,_) >> repos
		
		and:
		def items = new JsonSlurper().parseText(this.getClass().getResource('/testdata/items.json').text)
		4 * codeManagementService.listTopLevel(_,_,_) >> items
		4 * codeManagementService.ensureDeployManifest(_,_,_) >> items
		
		def team = new JsonSlurper().parseText(this.getClass().getResource('/testdata/projectteam.json').text)

		def folder =""
	
		and:
		String json = this.getClass().getResource('/testdata/builddefinitions.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		8 * genericRestClient.get(_) >> out
	
		when:
		def result = underTest.ensureBuilds('', 'project', folder, team)
		
		then:
		result.size() == 4
	}
	
	@Test
	def 'detectBuildType gradle success flow' () {
		given:
		def items = new JsonSlurper().parseText(this.getClass().getResource('/testdata/items.json').text)
		1 * codeManagementService.listTopLevel(_,_,_) >> items
		
		def pomitems = new JsonSlurper().parseText(this.getClass().getResource('/testdata/pomitems.json').text)
		1 * codeManagementService.listTopLevel(_,_,_) >> pomitems
		
		def nodeitems = new JsonSlurper().parseText(this.getClass().getResource('/testdata/nodeitems.json').text)
		1 * codeManagementService.listTopLevel(_,_,_) >> nodeitems
		
		def antitems = new JsonSlurper().parseText(this.getClass().getResource('/testdata/antitems.json').text)
		1 * codeManagementService.listTopLevel(_,_,_) >> antitems
		
		and:
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repos.json').text)
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		
		
		when:
		def resultGradle = underTest.detectBuildType('', project, repo)
		def resultMaven = underTest.detectBuildType('', project, repo)
		def resultNode = underTest.detectBuildType('', project, repo)
		def resultAnt = underTest.detectBuildType('', project, repo)
		
		then:
		"GRADLE" == resultGradle.toString()
		"MAVEN" == resultMaven.toString()
		"NODE" == resultNode.toString()
		"ANT" == resultAnt.toString()
	}
	
	@Test
	def 'ensureBuild successflow' () {
		given:
		String json = this.getClass().getResource('/testdata/builddefinitionscountzero.json').text
		JsonSlurper js = new JsonSlurper()
		def out = js.parseText(json)
		1 * genericRestClient.get(_) >> out
		
		and:
		String json1 = this.getClass().getResource('/testdata/buildConfigurations.json').text
		JsonSlurper js1 = new JsonSlurper()
		def out1 = js.parseText(json1)
		1 * genericRestClient.get(_) >> out1
		
		and:
		String json2 = this.getClass().getResource('/testdata/buildqueues.json').text
		JsonSlurper js2 = new JsonSlurper()
		def out2 = js.parseText(json2)
		1 * genericRestClient.get(_) >> out2
		
		
		def project = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		def repo = new JsonSlurper().parseText(this.getClass().getResource('/testdata/repos.json').text)
		def folder =""
		def buildStage = ""
		
		when:
		def result = underTest.ensureBuild('', project, repo, BuildType.GRADLE, '', folder)
		
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