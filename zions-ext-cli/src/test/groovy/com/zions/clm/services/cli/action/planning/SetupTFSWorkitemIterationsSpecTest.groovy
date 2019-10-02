package com.zions.clm.services.cli.action.planning

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

import com.zions.clm.services.ccm.client.RtcRepositoryClient
import com.zions.clm.services.ccm.project.planning.PlanManagementService
import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.rest.IGenericRestClient;
import com.zions.common.services.test.SpockLabeler
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.endpoint.EndpointManagementService
import com.zions.vsts.services.permissions.PermissionsManagementService
import com.zions.vsts.services.work.planning.IterationManagementService
import groovy.json.JsonSlurper

@ContextConfiguration(classes=[SetupTFSWorkitemIterationsTestConfig])
public class SetupTFSWorkitemIterationsSpecTest extends Specification implements SpockLabeler {
	
	@Autowired
	IGenericRestClient genericRestClient;
	
	@Autowired
	RtcRepositoryClient rtcRepositoryClient
	
	@Autowired
	IterationManagementService iterationManagementService;
	
	@Autowired
	PlanManagementService planManagementService
	
	@Autowired
	ProjectManagementService projectManagementService
	
	@Autowired
	SetupTFSWorkitemIterations underTest
	
	@Autowired
	CodeManagementService codeManagmentService
	
	@Autowired
	PermissionsManagementService permissionsManagementService
	
	@Autowired
	EndpointManagementService endpointManagementService
	
	@Autowired
	MemberManagementService memberManagementService
	
	@Autowired
	CommandManagementService commandManagementService
	
	@Autowired
	CodeManagementService codeManagementService
	
	
	@Test
	def 'validate ApplicationArguments success flow.'() {
		given: g_ 'valid Application Arguments'
		String[] args = ['--clm.url=http://localhost:8080', '--clm.user=user', '--clm.password=password', 
			'--ccm.projectArea=project_area', '--tfs.url=http://localhost:8080/tfs', '--tfs.user=tfsuser', 
			'--tfs.token=tfstoken', '--tfs.project=tfsproject', '--tfs.root.area=tfsrootareas' ]
		def appArgs = new DefaultApplicationArguments(args)
		
		
		when: w_ 'calling of method under test (validate)'
		def result = underTest.validate(appArgs)
		
		then: t_ 'result == true'
		result == true
	}
	
	@Test
	def 'validate ApplicationArguments exception flow.'() {
		given: g_ 'invalid Application Arguments'
		String[] args = ['--clm.url=http://localhost:8080']
		def appArgs = new DefaultApplicationArguments(args)
		
		when: w_ 'calling of method under test (validate)'
		def result = underTest.validate(appArgs)
		
		then: t_ 'thrown Exception'
		thrown Exception
	}
	
	@Test
	def 'execute ApplicationArguments success flow.' () {
		given: 'valid Application Arguments'
		String[] args = ['--clm.url=http://localhost:8080', '--clm.user=user', '--clm.password=password',
			'--ccm.projectArea=project_area', '--tfs.url=http://localhost:8080/tfs', '--tfs.user=tfsuser',
			'--tfs.token=tfstoken', '--tfs.project=tfsproject', '--tfs.root.area=tfsrootareas' ]
		def appArgs = new DefaultApplicationArguments(args)
		
		and: a_ 'Stub with project data'
		def theProject = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		projectManagementService.getProject(_, _) >> theProject
		
		and: a_ 'stub getIterations'
		def iterationData = new JsonSlurper().parseText(this.getClass().getResource('/testdata/iteration.json').text)
		planManagementService.getIterations(_,_) >> iterationData
		
		and: a_ 'stub getIterationData'
		def tfsIterationData
		iterationManagementService.getIterationData(_, _) >> tfsIterationData
		
		and: a_ 'stub processIterationData'
		iterationManagementService.processIterationData(_,_,_,_,_)
		
		and: a_ 'stub ensureTeamsIterations'
		iterationManagementService.ensureTeamsIterations(_,_,_,_)
		
		when: w_ 'calling of method under test (validate)'
		def result = underTest.execute(appArgs)
		
		then: t_ null
		result == null
	}

}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class SetupTFSWorkitemIterationsTestConfig {
	def factory = new DetachedMockFactory()
	
	@Bean
	IGenericRestClient genericRestClient() {
		return factory.Mock(ClmGenericRestClient)
	}
	
	@Bean
	MemberManagementService memberManagementService() {
		return factory.Mock(MemberManagementService)
	}
	
	@Bean
	IterationManagementService iterationManagementService() {
		return factory.Mock(IterationManagementService)
	}
	
	@Bean
	PlanManagementService planManagementService() {
		return factory.Mock(PlanManagementService)
	}
	
	@Bean
	RtcRepositoryClient rtcRepositoryClient() {
		return factory.Mock(RtcRepositoryClient)
	}
	
	@Bean
	CodeManagementService codeManagementService() {
		return factory.Mock(CodeManagementService)
	}
	
	@Bean
	ProjectManagementService projectManagementService() {
		return factory.Mock(ProjectManagementService)
	}
	
	@Bean
	EndpointManagementService endpointManagementService() {
		return factory.Mock(EndpointManagementService)
	}
	
	@Bean
	PermissionsManagementService permissionsManagementService() {
		return factory.Mock(PermissionsManagementService)
	}
	
	@Bean
	CommandManagementService commandManagementService() {
		return factory.Mock(CommandManagementService)
	}
	
	@Bean
	SetupTFSWorkitemIterations underTest() {
		return new SetupTFSWorkitemIterations()
	}

}
