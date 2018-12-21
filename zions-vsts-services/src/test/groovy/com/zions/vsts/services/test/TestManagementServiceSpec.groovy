package com.zions.vsts.services.test

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient
import com.zions.vsts.services.work.WorkManagementServiceConfig

import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[TestManagementServiceSpecConfig])
class TestManagementServiceSpec extends Specification {
	@Autowired(required=true)
	private IGenericRestClient genericRestClient;
	
	@Autowired(required=true)
	private ProjectManagementService projectManagmentService;

	@Autowired
	ICacheManagementService cacheManagementService
	
	@Autowired
	TestManagementService underTest

	def 'ensureResultAttachments success sending attachment to ADO' () {
		given: 'Stub call to cacheManagementService.getFromCache'
		
		and: 'Stub call to genericRestClient.get that gets attachments inside of hasAttachment (no attachment found)'
		
		and: 'Stub call to genericRestClient.post that send attachment to ADO'
		
		when: 'make call to method under test ensureResultAttachments'
		
		then: 'validate result of attachment Request'
	}
	
	def 'ensureResultAttachments attachment exist in ADO' () {
		given: 'Stub call to cacheManagementService.getFromCache'
		
		and: 'Stub call to genericRestClient.get that gets attachments inside of hasAttachment (attachment found)'
				
		when: 'make call to method under test ensureResultAttachments'
		
		then: 'validate result of attachment Request'
	}

}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class TestManagementServiceSpecConfig {
	def mockFactory = new DetachedMockFactory()
	
	@Bean
	IGenericRestClient genericRestClient() {
		return mockFactory.Mock(GenericRestClient)
	}
	
	@Bean
	ProjectManagementService projectManagmentService() {
		return mockFactory.Mock(ProjectManagementService)
	}
	
	@Bean
	ICacheManagementService cacheManagementService() {
		return mockFactory.Mock(ICacheManagementService)
	}
	
	@Bean
	TestManagementService underTest() {
		return new TestManagementService()
	}
	
}