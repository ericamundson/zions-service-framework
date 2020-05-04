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
import com.zions.common.services.test.SpockLabeler
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
	
	
	def 'validate method success flow.'() {
		
		given: 'valid Application Arguments'
		def appArgs = new DefaultApplicationArguments(args)


		when: 'calling of method under test (validate)'
		def result = underTest.validate(appArgs)

		then: 'result == true'
		result == true
	}
	
	
	def 'validate method exception flow.'() {
		
		given:'valid Application Arguments'
		String[] args = ['--bb.user=user']
		def appArgs = new DefaultApplicationArguments(args)

		when: 'calling of method under test (validate)'
		def result = underTest.validate(appArgs)

		then: 'No exception'
		thrown Exception
	}
	
	
	def 'execute method success flow.' () {
		
		given: 'valid Application Arguments'
		def appArgs = new DefaultApplicationArguments(args)
		
		and: 'stub extractWorkitemMetadata'
		def sample =[]
		ccmWIMetadataManagementService.extractWorkitemMetadata( _ , _) >> []
		
		when: 'calling of method under test (validate)'
		def result = underTest.execute(appArgs)

		then: 'thrown Exception'
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

