package com.zions.bb.services.cli.action.code;

import org.codehaus.groovy.ant.Groovy;
import org.junit.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.vsts.services.tfs.rest.GenericRestClient
import com.zions.bb.services.code.BBCodeManagementService
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.rest.IGenericRestClient;
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.endpoint.EndpointManagementService
import com.zions.vsts.services.permissions.PermissionsManagementService
import groovy.json.JsonSlurper
import spock.lang.Ignore
import spock.lang.IgnoreIf
import spock.lang.Specification;
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[SyncBBGitReposTestConfig])
public class SyncBBGitReposTest extends Specification{
	
	@Autowired
	IGenericRestClient genericRestClient;
	
	@Autowired
	IGenericRestClient bBGenericRestClient;
	
	@Autowired
	SyncBBGitRepos underTest
	
	@Autowired(required=true)
	CodeManagementService codeManagmentService
	
	@Autowired
	CommandManagementService commandManagementService
	
	@Autowired(required=true)
	PermissionsManagementService permissionsManagementService
	
	@Autowired(required=true)
	ProjectManagementService projectManagementService
	
	@Autowired(required=true)
	EndpointManagementService endpointManagementService
	
	@Autowired(required=true)
	MemberManagementService memberManagementService
	
	@Autowired(required=true)
	BBCodeManagementService bBCodeManagmentService
	
	String[] args = [
			'--tfs.url=http://localhost/tfs',
			'--tfs.collection=defaultcollection',
			'--tfs.user=z091182',
			'--tfs.token=hdepqrjz7sv4eewmqg4o55tflqndwbsue7yocki5xl5p5sqh6jxq',
			'--bb.project=almops',
			'--tfs.project=DigitalBanking',
			'--bb.url=http://localhost:8080/bb',
			'--grant.template=builder',
			'--tfs.team=ALMOps',
			'--bb.user=user',
			'--bb.password=password',
			'--repo.dir=/git',
			'--grant.template=password',
			'--clm.url=http://',
			'--clm.user=user',
			'--clm.password=password',
			'--qm.projectArea=src',
			'--qm.template.dir=gradle'
		]
		
	
	def 'validate method success flow.'() {
		given: "A stub of RQM get test item request"
		
		def appArgs = new DefaultApplicationArguments(args)
		
		when: 'calling of method under test (validate)'
		def testPlans = underTest.validate(appArgs)
		
		then: ''
		true
	}
	
	@Test
	def 'validate method exception flow.'() {
		
		given:'Stub with Application Arguments'
		String[] args = ['--tfs.collection=defaultcollection']
		def appArgs = new DefaultApplicationArguments(args)
		
		when: 'calling of method under test (validate)'
		def result = underTest.validate(appArgs)

		then:
		thrown Exception
	}
	
	@Test
	def 'execute method exception flow.'() {
		
		given:'Stub with Application Arguments'
		def appArgs = new DefaultApplicationArguments(args)
		
		def testplan = new JsonSlurper().parseText(getClass().getResource('/testdata/allprojects.json').text)
		(1..3) * bBGenericRestClient.get(_) >> testplan
		
		def test = new JsonSlurper().parseText(getClass().getResource('/testdata/allprojects_lastpage_false.json').text)
		1 * bBGenericRestClient.get(_) >> test
			
		when: 'calling of method under test (validate)'
		def result = underTest.execute(appArgs)

		then:
		thrown Exception
		
	}
	
	@Test
	def 'execute method filecheck flow.'() {
		
		given:'Stub with Application Arguments'
		String[] args = ['--bb.project=project', '--tfs.project=project', '--bb.password=password',
			'--bb.user=user', '--bb.url=http://', '--tfs.url=http://localhost:8080/tfs', '--tfs.user=tfsuser',
			'--tfs.token=tfstoken', '--grant.template=stuff', '--tfs.team=dumb' ]
		def appArgs = new DefaultApplicationArguments(args)
		
		def testplan = new JsonSlurper().parseText(getClass().getResource('/testdata/allprojects_projectrepo.json').text)
		(1..3) * bBGenericRestClient.get(_) >> testplan
		
		when: 'calling of method under test (validate)'
		def result = underTest.execute(appArgs)
		
		then:
		true
	}
	
	@Test
	def 'execute method ProjectRepoUrls flow.'() {
		
		given:'Stub with Application Arguments'
		def appArgs = new DefaultApplicationArguments(args)
		
		def testplan = new JsonSlurper().parseText(getClass().getResource('/testdata/allprojects_projectrepo.json').text)
		(1..3) * bBGenericRestClient.get(_) >> testplan
		
		and:
		def dummy =[]
		codeManagmentService.importRepoCLI( _, _, _, _, _) >> dummy
		
		and:
		def dummy1 =[]
		permissionsManagementService.ensureTeamToRepo(_, _, _, _, _) >> dummy1
		
		when: 'calling of method under test (validate)'
		def result = underTest.execute(appArgs)
		
		then:
		true
	}
	
	

}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class SyncBBGitReposTestConfig {
	def factory = new DetachedMockFactory()
	
	@Bean
	IGenericRestClient genericRestClient() {
		return factory.Mock(GenericRestClient)
	}
	
	@Bean
	IGenericRestClient bBGenericRestClient() {
		return factory.Mock(ClmGenericRestClient)
	}
	
	@Bean
	PermissionsManagementService permissionsManagementService() {
		return factory.Mock(PermissionsManagementService)
	}
	
	@Bean
	ProjectManagementService projectManagementService() {
		return new ProjectManagementService()
	}
	
	@Bean
	EndpointManagementService endpointManagementService() {
		return new EndpointManagementService()
	}
	
	@Bean
	CodeManagementService codeManagmentService() {
			return factory.Mock(CodeManagementService)
	}
	
	@Bean
	CommandManagementService commandManagementService() {
		return new CommandManagementService()
	}
	
	@Bean
	MemberManagementService memberManagementService() {
		return new MemberManagementService()
	}
	
	@Bean
	BBCodeManagementService bBCodeManagmentService() {
		return new BBCodeManagementService()
	}
	
	@Bean
	SyncBBGitRepos underTest() {
		return new SyncBBGitRepos(CodeManagementService codeManagmentService,
		BBCodeManagementService bBCodeManagmentService,
		PermissionsManagementService permissionsManagementService)
	}

}
