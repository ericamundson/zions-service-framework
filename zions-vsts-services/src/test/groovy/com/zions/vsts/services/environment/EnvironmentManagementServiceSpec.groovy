package com.zions.vsts.services.environment

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ActiveProfiles

import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.mr.services.rest.MrGenericRestClient
import com.zions.vsts.services.attachments.AttachmentManagementService
import com.zions.vsts.services.tfs.rest.MultiUserGenericRestClient

import spock.lang.Specification

@SpringBootTest(classes=[EnvironmentManagementServiceConfig])
@ActiveProfiles('k8stest')
class EnvironmentManagementServiceSpec extends Specification {
	@Autowired
	ProjectManagementService projectManagementService
	
	@Autowired
	EnvironmentManagementService environmentManagementService

	def 'ensure a environment'() {
		given: 'A test environment name.'
		String name = 'a_test_environment'
		
		and: 'ado project'
		def proj = projectManagementService.getProject('', 'DTS')
		
		when: 'ensure that environment'
		boolean error = false
		def env = null
		try {
			env = environmentManagementService.ensureEnvironment('', proj, name, 'new env')
		} catch (e) {
			error = true
		}
		
		then: 'environment is created with name.'
		!error && env.name == name
		
//		cleanup:
//		environmentManagementService.deleteEnvironment('', proj, name)
		
	}

}

@ComponentScan(["com.zions.vsts.services.environment", "com.zions.vsts.services.admin.project"])
@PropertySource("classpath:test.properties")
class EnvironmentManagementServiceConfig {
	@Value('${tfs.url:}')
	String tfsUrl
	@Value('${tfs.user:}')
	String tfsUser
	@Value('${tfs.token:}')
	String tfsToken
	@Bean
	ICacheManagementService cacheManagementService() {
		return new CacheManagementService(cacheLocation)
	}

	@Bean
	CommandManagementService commandManagementService() {
		return new CommandManagementService();
	}

//	@Bean
//	AttachmentManagementService attachmentManagementService() {
//		return new AttachmentManagementService();
//	}
	
	@Bean
	IGenericRestClient genericRestClient() {
		MultiUserGenericRestClient c = new MultiUserGenericRestClient()
		return c
	}

	@Bean
	IGenericRestClient mrGenericRestClient() {
		return new MrGenericRestClient('', '')
	}


	@Autowired
	@Value('${cache.location:cache}')
	String cacheLocation

}