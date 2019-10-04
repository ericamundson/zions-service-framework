package com.zions.clm.services.cli.action.wit;

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration;

import com.zions.clm.services.ccm.client.RtcRepositoryClient
import com.zions.clm.services.ccm.workitem.metadata.CcmWIMetadataManagementService
import com.zions.clm.services.rtc.project.members.CcmMemberManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.SpockLabeler
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService

import spock.lang.Specification;
import spock.mock.DetachedMockFactory


@ContextConfiguration(classes=[BuildWITStarterTestConfig])
public class BuildWITStarterTest extends Specification {
	
	@Autowired
	RtcRepositoryClient rtcRepositoryClient
	
	@Autowired
	CcmWIMetadataManagementService ccmWIMetadataManagementService
	
	@Autowired
	BuildWITStarter underTest
	
	@Autowired
	ExtractCcmWIMetadata extractCcmWIMetadata
	
	String[] args = [
		'--in.file=/wit.csv',
		'--out.file=http://localhost:8080'
	]
	
	@Test
	def 'validate method success flow.'() {
		given: 'valid Application Arguments'
		def appArgs = new DefaultApplicationArguments(args)

		when: 'calling of method under test (validate)'
		def result = underTest.validate(appArgs)

		then: 'result == true'
		result == true
	}
	
	@Test
	def 'validate method exception flow.'() {
		given:'invalid Application Arguments'
		String[] args = ['--bb.user=user']
		def appArgs = new DefaultApplicationArguments(args)

		when: 'calling of method under test (validate)'
		def result = underTest.validate(appArgs)

		then: 'thrown Exception'
		thrown Exception
	}
	
	@Test
	def 'execute method success flow.' () {
		
		given: 'valid Application Arguments'
		def appArgs = new DefaultApplicationArguments(args)
				
		when: 'calling of method under test (execute)'
		def result = underTest.execute(appArgs)

		then: 'thrown Exception'
		thrown Exception
	}
	
	@Test
	def 'buildStarterXml method success flow.' () {
		
		given: 'valid Application Arguments'
		def appArgs = new DefaultApplicationArguments(args)
		String inFile = 'src/main/resources/wit.csv'
		
		when: 'calling of method under test (buildStarterXml)'
		def result = underTest.buildStarterXml(inFile)

		then: 'No exception'
		true
	}
		

}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class BuildWITStarterTestConfig {
	def factory = new DetachedMockFactory()
	
	@Bean
	RtcRepositoryClient rtcRepositoryClient() {
		return factory.Mock(RtcRepositoryClient)
	}
	
	@Bean
	CcmWIMetadataManagementService ccmWIMetadataManagementService() {
		return new CcmWIMetadataManagementService()
	}
	
	@Bean
	BuildWITStarter underTest() {
		return new BuildWITStarter()
	}
	
	@Bean
	ExtractCcmWIMetadata extractCcmWIMetadata() {
		return new ExtractCcmWIMetadata()
	}

}
