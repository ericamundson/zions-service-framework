package com.zions.clm.services.cli.action.wit;

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import com.zions.clm.services.ccm.client.RtcRepositoryClient
import com.zions.clm.services.ccm.workitem.metadata.CcmWIMetadataManagementService
import com.zions.clm.services.rtc.project.members.CcmMemberManagementService
import com.zions.common.services.rest.IGenericRestClient;

import spock.lang.Specification;
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[ExtractCcmWIMetadataTestConfig])
public class ExtractCcmWIMetadataTest extends Specification {
	
	@Autowired
	ExtractCcmWIMetadata underTest
	
	@Autowired
	RtcRepositoryClient rtcRepositoryClient
	
	@Autowired
	CcmWIMetadataManagementService ccmWIMetadataManagementService
	
	@Test
	def 'validate method success flow.'() {
		
		given: g_ 'Stub Application Arguments'
		def appArgs = new DefaultApplicationArguments(args)


		when: w_ 'calling of method under test (validate)'
		def result = underTest.validate(appArgs)

		then: t_ 'result == true'
		result == true
	}
	
	@Test
	def 'validate method exception flow.'() {
		
		given:'Stub Application Arguments'
		String[] args = ['--bb.user=user']
		def appArgs = new DefaultApplicationArguments(args)

		when: w_ 'calling of method under test (validate)'
		def result = underTest.validate(appArgs)

		then: t_ 'thrown Exception'
		thrown Exception
	}
	
	@Test
	def 'execute method success flow.' () {
		
		given: 'Stub Application Arguments'
		def appArgs = new DefaultApplicationArguments(args)
		
		and: a_ 'stub extractWorkitemMetadata'
		def sample =[]
		ccmWIMetadataManagementService.extractWorkitemMetadata( _ , _) >> []
		
		when: w_ 'calling of method under test (validate)'
		def result = underTest.execute(appArgs)

		then: t_ 'thrown Exception'
		thrown Exception
	}
	
	String[] args = [
			'--clm.url=http://localhost:8080',
			'--clm.user=user',
			'--clm.password=password',
			'--clm.projectArea=ALMOps',
			'--template.dir=src'
			]
	

}


@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class ExtractCcmWIMetadataTestConfig {
	def factory = new DetachedMockFactory()
	
	@Bean
	RtcRepositoryClient rtcRepositoryClient() {
		return factory.Mock(RtcRepositoryClient)
	}
	
	@Bean
	CcmWIMetadataManagementService ccmWIMetadataManagementService() {
		return factory.Mock(CcmWIMetadataManagementService)
	}
	
	@Bean
	ExtractCcmWIMetadata underTest() {
		return new ExtractCcmWIMetadata()
	}

}

