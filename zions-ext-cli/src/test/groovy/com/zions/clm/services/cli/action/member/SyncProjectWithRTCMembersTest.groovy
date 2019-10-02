package com.zions.clm.services.cli.action.member;

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.clm.services.rtc.project.members.CcmMemberManagementService
import com.zions.common.services.rest.IGenericRestClient;
import com.zions.common.services.test.SpockLabeler
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.endpoint.EndpointManagementService
import com.zions.vsts.services.permissions.PermissionsManagementService

import groovy.json.JsonSlurper
import spock.lang.Specification;
import spock.mock.DetachedMockFactory



@ContextConfiguration(classes=[SyncProjectWithRTCMembersTestConfig])
public class SyncProjectWithRTCMembersTest extends Specification implements SpockLabeler {
	
	@Autowired
	SyncProjectWithRTCMembers underTest
	
	@Autowired
	IGenericRestClient clmGenericRestClient
	
	@Autowired
	CcmMemberManagementService ccmMemberManagmentService
	
	@Autowired
	MemberManagementService memberManagementService
	
	@Autowired
	ProjectManagementService projectManagementService
	
	@Test
	def 'validate method exception flow.'() {
		
		given: g_ 'invalid Application Arguments'
		String[] args = ['--tfs.collection=defaultcollection']
		def appArgs = new DefaultApplicationArguments(args)

		when: w_ 'calling of method under test (validate)'
		def result = underTest.validate(appArgs)

		then: t_ 'thrown Exception'
		thrown Exception
	}
	
	@Test
	def 'validate method success flow.'() {
		
		given: g_ 'valid Application Arguments'
		def appArgs = new DefaultApplicationArguments(args)


		when: w_ 'calling of method under test (validate)'
		def result = underTest.validate(appArgs)

		then: t_ 'result == true'
		result == true
	}
	
	@Test
	def 'execute method success flow.' () {
		
		given: g_ 'Valid Application Arguments'
		def appArgs = new DefaultApplicationArguments(args)
		
		and: a_ 'stub rest call for test plans'
		def testplansInfo = new XmlSlurper().parseText(this.getClass().getResource('/testdata/ccmworkitemtype.xml').text)
		1 * clmGenericRestClient.get(_) >> testplansInfo

		when: w_ 'call execute'
		def result = underTest.execute(appArgs)

		then: t_ 'result == null'
		result == null
	}
	
	@Test
	def 'rebuildMemberData method success flow.' () {
		
		given: 'valid Application Arguments'
		def teamInfo = new XmlSlurper().parseText(getClass().getResource('/testdata/testmember.xml').text)
		def map = new JsonSlurper().parseText(getClass().getResource('/testdata/mbteamnamemap.json').text)
				
		when: w_ 'calling of method under test (validate)'
		def result = underTest.rebuildMemberData(teamInfo,map)

		then: t_ null
		true
	}
	
		String[] args = [
			'--tfs.url=http://localhost:8080/tfs',
			'--tfs.collection=defaultcollection',
			'--clm.ccm.project=project',
			'--tfs.project=tfsproject',
			'--tfs.user=z091182',
			'--tfs.token=hdepqrjz7sv4eewmqg4o55tflqndwbsue7yocki5xl5p5sqh6jxq',
			'--clm.url=http://localhost:8080',
			'--clm.user=user',
			'--clm.password=password',
			'--clm.ccm.project=ALMOps',
			'--namemap.json.file=mbteamnamemap.json',
			'--qm.projectArea=src',
			'--qm.template.dir=gradle'
		]
		

}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class SyncProjectWithRTCMembersTestConfig {
	def factory = new DetachedMockFactory()
	
	@Bean
	IGenericRestClient clmGenericRestClient() {
		return factory.Mock(ClmGenericRestClient)
	}
	
	@Bean
	CcmMemberManagementService ccmMemberManagmentService() {
		return new CcmMemberManagementService()
	}
	
	@Bean
	MemberManagementService memberManagmentService() {
		return new MemberManagementService()
	}
	
	@Bean
	ProjectManagementService projectManagementService() {
		return new ProjectManagementService()
	}
	
	@Bean
	SyncProjectWithRTCMembers underTest() {
		return new SyncProjectWithRTCMembers(MemberManagementService memberManagmentService, CcmMemberManagementService ccmMemberManagmentService)
	}

}
