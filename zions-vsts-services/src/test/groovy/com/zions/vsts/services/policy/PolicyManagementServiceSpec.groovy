package com.zions.vsts.services.policy




import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration
import spock.mock.DetachedMockFactory
import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.admin.member.MemberManagementServiceTestConfig
import com.zions.vsts.services.build.BuildManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient
import spock.lang.Ignore

/*
@ContextConfiguration(classes=[PolicyManagementServiceSpecConfig])
class PolicyManagementServiceSpec {
	@Autowired
	BuildManagementService buildManagementService
	
	@Autowired
	IGenericRestClient genericRestClient
	
	@Autowired
	PolicyManagementService underTest
	
	@Ignore
	def 'ensurePolicies main flow.'() {
//		when:  'buildManagementService.ensureBuildsForBranch stub'
//		1 * buildManagementService.ensureBuildsForBranch(_, _, _) >> [ciBuildId: 3456]
	}

}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class PolicyManagementServiceSpecConfig {
	def mockFactory = new DetachedMockFactory()
	
	
	@Bean
	IGenericRestClient genericRestClient() {
		return mockFactory.Mock(GenericRestClient, name: 'genericRestClient')
	}
	
	@Bean
	BuildManagementService buildManagementService() {
		return mockFactory.Mock(BuildManagementService)
	}

	@Bean underTest() {
		return new PolicyManagementService()
	}
}
*/