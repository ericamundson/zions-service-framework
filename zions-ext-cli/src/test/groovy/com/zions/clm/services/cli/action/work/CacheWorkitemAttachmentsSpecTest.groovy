package com.zions.clm.services.cli.action.work

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
import com.zions.clm.services.ccm.workitem.attachments.AttachmentsManagementService
import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.clm.services.rtc.project.workitems.ClmWorkItemManagementService
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

@ContextConfiguration(classes=[CacheWorkitemAttachmentsTestConfig])
public class CacheWorkitemAttachmentsSpecTest extends Specification {

	@Autowired
	IGenericRestClient genericRestClient;

	@Autowired
	AttachmentsManagementService attachmentsManagementService

	@Autowired
	RtcRepositoryClient rtcRepositoryClient

	@Autowired
	ClmWorkItemManagementService clmWorkItemManagementService

	@Autowired
	CacheWorkitemAttachments underTest

	@Test
	def 'validate ApplicationArguments success flow.'() {
		given: 'Stub with Application Arguments'
		String[] args = loadArgs()
		def appArgs = new DefaultApplicationArguments(args)


		when: 'calling of method under test (validate)'
		def result = underTest.validate(appArgs)

		then: ''
		result == true
	}

	private String[] loadArgs() {
		String[] args = [
			'--clm.url=http://localhost:8080',
			'--clm.user=user',
			'--clm.password=password',
			'--ccm.projectArea=src'
		]
		return args
	}

	@Test
	def 'validate ApplicationArguments exception flow.'() {
		given:'Stub with Application Arguments'
		String[] args = ['--clm.url=http://localhost:8080']
		def appArgs = new DefaultApplicationArguments(args)
		
		when: 'calling of method under test (validate)'
		def result = underTest.validate(appArgs)
		
		then:
		thrown Exception
	}

	@Test
	def 'execute ApplicationArguments success flow.' () {
		given: 'Stub with Application Arguments'
		String[] args = loadArgs()
		def appArgs = new DefaultApplicationArguments(args)
		
		and:
		def workItems
		clmWorkItemManagementService.getWorkItemsViaQuery(_) >> workItems
		
		and:
		//attachmentsManagementService.cacheWorkItemAttachments(_)
		
		and:
		//clmWorkItemManagementService.nextPag(_)
		//attachmentsManagementService.rtcRepositoryClient.shutdownPlatform()



		when: 'calling of method under test (validate)'
		def result = underTest.execute(appArgs)

		then: ''
		result == null
	}
}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class CacheWorkitemAttachmentsTestConfig {
	def factory = new DetachedMockFactory()

	@Bean
	IGenericRestClient genericRestClient() {
		return factory.Mock(ClmGenericRestClient)
	}

	@Bean
	AttachmentsManagementService attachmentsManagementService() {
		return new AttachmentsManagementService()
	}

	@Bean
	RtcRepositoryClient rtcRepositoryClient() {
		return factory.Mock(RtcRepositoryClient)
	}

	@Bean
	ClmWorkItemManagementService clmWorkItemManagementService() {
		return new ClmWorkItemManagementService()
	}

	@Bean
	CacheWorkitemAttachments underTest() {
		return new CacheWorkitemAttachments(AttachmentsManagementService attachmentsManagementService,
				ClmWorkItemManagementService clmWorkItemManagementService)
	}
}
