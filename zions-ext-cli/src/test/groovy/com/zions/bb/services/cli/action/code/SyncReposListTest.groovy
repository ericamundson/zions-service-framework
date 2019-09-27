package com.zions.bb.services.cli.action.code;

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import spock.lang.Specification
import spock.mock.DetachedMockFactory
import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.common.services.rest.IGenericRestClient;
import com.zions.vsts.services.tfs.rest.GenericRestClient
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.common.services.command.CommandManagementService
import com.zions.vsts.services.endpoint.EndpointManagementService
import com.zions.vsts.services.permissions.PermissionsManagementService
import groovy.json.JsonSlurper

@ContextConfiguration(classes=[SyncReposListTestConfig])
public class SyncReposListTest extends Specification {
	
	@Autowired
	IGenericRestClient genericRestClient;
	
	@Autowired
	SyncReposList underTest
	
	@Autowired
	CodeManagementService codeManagmentService
	
	@Autowired
	CommandManagementService commandManagementService
	
	@Autowired
	PermissionsManagementService permissionsManagementService
	
	@Autowired
	ProjectManagementService projectManagementService
	
	@Autowired
	EndpointManagementService endpointManagementService
	
	@Autowired
	MemberManagementService memberManagementService
	
	String[] args = [
		'--tfs.url=http://localhost:8080/tfs',
		'--tfs.collection=defaultcollection',
		'--tfs.user=z091182',
		'--tfs.token=hdepqrjz7sv4eewmqg4o55tflqndwbsue7yocki5xl5p5sqh6jxq',
		'--tfs.project=Zions Tools Project',
		'--grant.template=builder',
		'--tfs.team=ALMOps',
		'--in.urls=http://localhost:8080/tfs',
		'--in.user=user',
		'--in.password=password',
		'--repo.dir=src/test/resources/testdata',
		'--grant.template=password'
	]
	
	@Test
	def 'validate method exception flow.'() {
		
		given: g_ 'Stub with Application Arguments'
		String[] args = ['--tfs.collection=defaultcollection']
		def appArgs = new DefaultApplicationArguments(args)

		when: w_ 'calling of method under test (validate)'
		def result = underTest.validate(appArgs)

		then: t_ 'thrown Exception'
		thrown Exception
	}
	
	@Test
	def 'validate method  flow.'() {
		
		given: g_ 'Stub with Application Arguments'
		String[] args = []
		def appArgs = new DefaultApplicationArguments(args)

		when: w_ 'call validate'
		def result = underTest.validate(appArgs)

		then: t_ 'thrown Exception'
		thrown Exception
	}
	
	@Test
	def 'validate method success flow.'() {
		
		given: "A stub of RQM get test item request"		
		def appArgs = new DefaultApplicationArguments(args)
		
		when: w_ 'call validate'
		def testPlans = underTest.validate(appArgs)
		
		then: t_ null
		true
	}
	
	@Test
	def 'execute method  flow.'() {
		
		given: g_ 'Stub with Application Arguments'
		String[] args = []
		def appArgs = new DefaultApplicationArguments(args)

		when: w_ 'call execute'
		def result = underTest.execute(appArgs)

		then: t_ 'thrown Exception'
		thrown Exception
	}
	
	
	@Test
	def 'execute method success flow.'() {
		
		given: g_ 'Stub with Application Arguments'
		def appArgs = new DefaultApplicationArguments(args)
		
		and: a_ 'stub importRepoDir call'
		def dummy =[]
		codeManagmentService.importRepoDir(_, _, _, _, _, _) >> dummy
		
		and: a_ 'stub call to ensureTeamToRepo'
		def dummy1 =[]
		permissionsManagementService.ensureTeamToRepo(_, _, _, _, _) >> dummy1
		
		when: w_ 'call execute'
		def result = underTest.execute(appArgs)

		then: t_ 'No exception'
		true
		
	}
		

}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class SyncReposListTestConfig {
	def factory = new DetachedMockFactory()
	
	@Bean
	CodeManagementService codeManagmentService() {
		return factory.Mock(CodeManagementService)
		//return new CodeManagementService()
	}
	
	@Bean
	CommandManagementService commandManagementService() {
		return new CommandManagementService()
	}
	
	@Bean
	IGenericRestClient genericRestClient() {
		return factory.Mock(GenericRestClient)
	}
	
	@Bean
	IGenericRestClient bBGenericRestClient() {
		return factory.Mock(ClmGenericRestClient)
	}
	
	@Bean
	MemberManagementService memberManagementService() {
		return new MemberManagementService()
	}
	
	@Bean
	EndpointManagementService endpointManagementService() {
		return new EndpointManagementService()
	}
	
	@Bean
	PermissionsManagementService permissionsManagementService() {
		return factory.Mock(PermissionsManagementService)
		//return new PermissionsManagementService()
	}
	
	@Bean
	ProjectManagementService projectManagementService() {
		return new ProjectManagementService()
	}
	
	@Bean
	SyncReposList underTest() {
		return new SyncReposList(CodeManagementService codeManagmentService, 
		PermissionsManagementService permissionsManagementService)
	}

}
