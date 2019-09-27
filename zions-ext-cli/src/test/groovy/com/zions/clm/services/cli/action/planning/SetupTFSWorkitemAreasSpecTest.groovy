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
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.endpoint.EndpointManagementService
import com.zions.vsts.services.permissions.PermissionsManagementService
import com.zions.vsts.services.work.planning.IterationManagementService
import com.zions.vsts.services.workitem.AreasManagementService

import groovy.json.JsonSlurper

@ContextConfiguration(classes=[SetupTFSWorkitemAreasTestConfig])
public class SetupTFSWorkitemAreasSpecTest extends Specification {
	
	@Autowired
	IGenericRestClient genericRestClient;
	
	@Autowired
	RtcRepositoryClient rtcRepositoryClient
	
	@Autowired
	AreasManagementService areasManagementService;
	
	@Autowired
	PlanManagementService planManagementService
	
	@Autowired
	ProjectManagementService projectManagementService
	
	@Autowired
	SetupTFSWorkitemAreas underTest
	
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
		given: g_ 'Stub with Application Arguments'
		String[] args = ['--clm.url=http://localhost:8080', '--clm.user=user', '--clm.password=password', 
			'--ccm.projectArea=project_area', '--tfs.url=http://localhost:8080/tfs', '--tfs.user=tfsuser', 
			'--tfs.token=tfstoken', '--tfs.project=tfsproject', '--tfs.root.area=tfsrootareas' ]
		def appArgs = new DefaultApplicationArguments(args)
		
		
		when: w_ 'calling of method under test (validate)'
		def result = underTest.validate(appArgs)
		
		then: t_ null
		result == true
	}
	
	@Test
	def 'validate ApplicationArguments exception flow.'() {
		given: g_ 'Stub with Application Arguments'
		String[] args = ['--clm.url=http://localhost:8080']
		def appArgs = new DefaultApplicationArguments(args)
		
		when: w_ 'calling of method under test (validate)'
		def result = underTest.validate(appArgs)
		
		then: t_ 'thrown Exception'
		thrown Exception
	}
	
	@Test
	def 'execute ApplicationArguments success flow.' () {
		given: g_ 'Stub with Application Arguments'
		String[] args = ['--clm.url=http://localhost:8080', '--clm.user=user', '--clm.password=password',
			'--ccm.projectArea=project_area', '--tfs.url=http://localhost:8080/tfs', '--tfs.user=tfsuser',
			'--tfs.token=tfstoken', '--tfs.project=tfsproject', '--tfs.root.area=tfsrootareas', '--tfs.collection=collection' ]
		def appArgs = new DefaultApplicationArguments(args)
		
		and: a_ 'Stub with project data'
		def theProject = new JsonSlurper().parseText(this.getClass().getResource('/testdata/project.json').text)
		projectManagementService.getProject(_, _) >> theProject
		
		and: a_ 'stub planManagementService.getCategories(_,_)'
		def areaData = new JsonSlurper().parseText(this.getClass().getResource('/testdata/areadata.json').text)
		planManagementService.getCategories(_,_)  >> areaData
		
		and: a_ 'stub other data processing calls'
		def tfsAreaData 
		areasManagementService.getAreaData(_,_) >> tfsAreaData
		areasManagementService.processAreasData(_, _, _, _, _)
		areasManagementService.assignTeamAreas(_, _, _)
		
		when: w_ 'calling of method under test (validate)'
		def result = underTest.execute(appArgs)
		
		then: t_ 'result == null'
		result == null
	}

}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class SetupTFSWorkitemAreasTestConfig {
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
	AreasManagementService areasManagementService() {
		return factory.Mock(AreasManagementService)
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
	SetupTFSWorkitemAreas underTest() {
		return new SetupTFSWorkitemAreas()
	}

}
