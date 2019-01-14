package com.zions.qm.services.cli.action.metadata

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
import com.zions.vsts.services.tfs.rest.GenericRestClient
import com.zions.qm.services.metadata.QmMetadataManagementService
import com.zions.qm.services.project.QmProjectManagementService
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.code.CodeManagementService
import com.zions.vsts.services.endpoint.EndpointManagementService
import com.zions.vsts.services.permissions.PermissionsManagementService
import com.zions.vsts.services.work.planning.IterationManagementService
import com.zions.vsts.services.workitem.AreasManagementService

import groovy.json.JsonSlurper

@ContextConfiguration(classes=[ExtractQmMetadataTestConfig])
public class ExtractQmMetadataSpecTest extends Specification {

	@Autowired
	IGenericRestClient genericRestClient;

	@Autowired
	QmMetadataManagementService qmMetadataManagementService;
	
	@Autowired
	QmProjectManagementService  qmProjectManagementSerivce
	
	@Autowired
	ExtractQmMetadata underTest

	@Test
	def 'validate ApplicationArguments success flow.'() {
		//TODO: validate ApplicationArguments success flow.
	}

	@Test
	def 'validate ApplicationArguments exception flow.'() {
		//TODO: validate ApplicationArguments exception flow.
	}

	@Test
	def 'execute ApplicationArguments success flow.' () {
		//TODO: execute ApplicationArguments success flow.
	}
}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class ExtractQmMetadataTestConfig {
	def factory = new DetachedMockFactory()

	@Bean
	IGenericRestClient genericRestClient() {
		return factory.Mock(GenericRestClient)
	}

	@Bean
	QmProjectManagementService  qmProjectManagementSerivce() {
		return factory.Mock(QmProjectManagementService)
	}

	@Bean
	ExtractQmMetadata underTest() {
		return new ExtractQmMetadata()
	}
}
