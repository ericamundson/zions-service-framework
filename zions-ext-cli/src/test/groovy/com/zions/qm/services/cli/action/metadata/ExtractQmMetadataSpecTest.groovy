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
import com.zions.common.services.test.SpockLabeler
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

	
	def 'validate ApplicationArguments success flow.'() {
		given: 'valid Application Arguments'
		String[] args = loadArgs()
		def appArgs = new DefaultApplicationArguments(args)


		when: 'calling of method under test (validate)'
		def result = underTest.validate(appArgs)

		then: 'No exception'
		result == true
	}

	private String[] loadArgs() {
		String[] args = [
			'--clm.url=http://localhost:8080',
			'--clm.user=user',
			'--clm.password=password',
			'--qm.projectArea=src',
			'--qm.template.dir=gradle'
		]
		return args
	}

	
	def 'validate ApplicationArguments exception flow.'() {
		given:'bad Application Arguments'
		String[] args = ['--clm.url=http://localhost:8080']
		def appArgs = new DefaultApplicationArguments(args)

		when: 'calling of method under test (validate)'
		def result = underTest.validate(appArgs)

		then: 'thrown Exception'
		thrown Exception
	}

	
	def 'execute ApplicationArguments success flow.' () {
		given: 'valid Application Arguments'
		def appArgs = new DefaultApplicationArguments(loadArgs())

		and: null
		qmMetadataManagementService.extractQmMetadata(_, _) >> "<a1>some meta<a1>"
		
		when: 'calling of method under test (validate)'
		def result = underTest.execute(appArgs)

		then: 'result = null'
		result == null
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
	QmMetadataManagementService qmMetadataManagementService() {
		return factory.Mock(QmMetadataManagementService)
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
