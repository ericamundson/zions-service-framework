package com.zions.ext.services.cli.action.rest

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

@ContextConfiguration(classes=[RestClientTestConfig])
public class RestClientSpecTest extends Specification {

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
	RestClient underTest

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
		//TODO:  write validate success flow
	}

	@Test
	def 'validate ApplicationArguments exception flow.'() {
		//TODO:  write validate exception flow
	}
	
	@Test
	def 'execute ApplicationArguments success flow.' () {
		//TODO:  write execute success flow
	}
}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class RestClientTestConfig {
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
	RestClient underTest() {
		return new RestClient()
	}
}
