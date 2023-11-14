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
import com.zions.mr.services.rest.MrGenericRestClient
import com.zions.vsts.services.attachments.AttachmentManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient

import spock.lang.Specification
import spock.lang.Ignore

@SpringBootTest(classes=[CheckConfigurationManagementServiceConfig])
@ActiveProfiles('k8stest')
class CheckConfigurationManagementServiceSpec extends Specification {

	@Autowired
	ProjectManagementService projectManagementService
	
	@Autowired
	EnvironmentManagementService environmentManagementService
	
	@Autowired
	CheckConfigurationManagementService checkConfigurationManagementService

	@Ignore
	def 'ensure a check configuration'() {
		given: 'A test environment name.'
		String name = 'check_config_env'
		
		and: 'ado project'
		def proj = projectManagementService.getProject('', 'DTS')
		
		when: 'ensure that environment and config'
		boolean error = false
		def env = null
		def config = null
		try {
			env = environmentManagementService.ensureEnvironment('', proj, name, 'descp check_config_env')
			config = checkConfigurationManagementService.ensureEnvCheckConfiguration('', proj, env, ['Eric Amundson', 'Sarmila Dangol'] as String[])
		} catch (e) {
			error = true
		}
		
		then: 'environment is created with name.'
		!error && env.name == name && config != null
		
//		cleanup:
//		environmentManagementService.deleteEnvironment('', proj, name)
		
	}

}

@ComponentScan(["com.zions.vsts.services", "com.zions.common.services.notification"])
//@PropertySource("classpath:test.properties")
class CheckConfigurationManagementServiceConfig {
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
		GenericRestClient c = new GenericRestClient(tfsUrl, tfsUser, tfsToken)
		return c
	}

	@Bean
	IGenericRestClient mrGenericRestClient() {
		return new MrGenericRestClient('', '')
	}
	
	@Bean
	ProjectManagementService projectManagementService() {
		ProjectManagementService s = new ProjectManagementService()
		s.genericRestClient = genericRestClient()
		return s
	}


	@Autowired
	@Value('${cache.location:cache}')
	String cacheLocation

}