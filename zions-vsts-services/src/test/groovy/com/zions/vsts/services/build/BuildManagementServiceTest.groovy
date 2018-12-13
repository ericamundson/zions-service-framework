package com.zions.vsts.services.build

import static org.junit.Assert.*

import groovy.json.JsonSlurper
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.member.MemberManagementServiceTestConfig
import com.zions.vsts.services.tfs.rest.GenericRestClient
import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[BuildManagementServiceTestConfig])
class BuildManagementServiceTest extends Specification {
	@Autowired
	IGenericRestClient genericRestClient
	
	@Autowired
	BuildManagementService buildManagementService

	@Test
	def 'createBuild success flow'() {
//		given: 'Stub request to get all build definition templates'
//		def entitlements = new JsonSlurper().parseText(this.getClass().getResource('/testdata/builddefinitiontemplates.json').text)
//		1 * genericRestClient.get(_) >> entitlements
//
//		when: 'call method under test createBuild'
//		def out = buildManagementService.createBuild('', 'DigitalBanking', repo, buildType, buildStage, folder)
//
//		then: 
//		true		
	}

}


@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class BuildManagementServiceTestConfig {
	def mockFactory = new DetachedMockFactory()
	
	@Bean
	IGenericRestClient genericRestClient() {
		return mockFactory.Mock(GenericRestClient, name: 'genericRestClient')
	}

	@Bean
	BuildManagementService underTest() {
		return new BuildManagementService();
	}
}