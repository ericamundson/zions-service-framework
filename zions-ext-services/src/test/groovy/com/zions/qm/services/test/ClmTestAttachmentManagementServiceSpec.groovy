package com.zions.qm.services.test

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration
import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.DataGenerationService
import com.zions.common.services.test.SpockLabeler
import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[ClmTestAttachmentManagementServiceSpecConfig])
class ClmTestAttachmentManagementServiceSpec extends Specification implements SpockLabeler {
	@Autowired
	IGenericRestClient qmGenericRestClient

	@Autowired
	ClmTestManagementService clmTestManagementService

	@Autowired
	ICacheManagementService cacheManagementService
	
	@Autowired
	ClmTestAttachmentManagementService underTest
	
	@Autowired
	DataGenerationService dataGenerationService

	@Autowired
	TestMappingManagementService testMappingManagementService


	def 'cacheTestItemAttachments main flow'() {
		given: g_ 'Stub of clmTestManagementService getContent call'
		String encodedStuff = "Here's some text".bytes.encodeBase64()
		ByteArrayInputStream s = new ByteArrayInputStream("Here's some text".bytes)
		1 * clmTestManagementService.getContent(_) >> [filename: 'file.txt', data: s]
		
		and: a_ 'Stub of cache management service save binary'
		File sFile = new File('stuff.txt')
//		def os = sFile.newDataOutputStream()
//		os << "Here's some text"
//		os.close()
		1 * cacheManagementService.saveBinaryAsAttachment(_,_,_) >> sFile
		
		and: a_ 'test plan with single attachment'
		def titem = dataGenerationService.generate('/testdata/testplanT.xml')
		
		when: w_ 'Calling method under test cacheTestItemAttachments'
		boolean success = true
		try {
			underTest.cacheTestItemAttachments(titem)
		} catch (e) {
			success = false
		}
		
		then: t_ 'ensure success'
	}
	
	def 'cacheTestCaseAttachments main flow'() {
		given: g_ 'Stub of clmTestManagementService getContent call'
		String encodedStuff = "Here's some text".bytes.encodeBase64()
		ByteArrayInputStream s = new ByteArrayInputStream("Here's some text".bytes)
		2 * clmTestManagementService.getContent(_) >> [filename: 'stuff.txt', data: s]
		
		and: a_ 'Stub of cache management service save binary'
		File sFile = new File('stuff.txt')
//		def os = sFile.newDataOutputStream()
//		os << "Here's some text"
//		os.close()
		2 * cacheManagementService.saveBinaryAsAttachment(_,_,_) >> sFile
		
		and: a_ 'test case with single attachment'
		def titem = dataGenerationService.generate('/testdata/testcaseT.xml')
		
		and: a_ 'Stub return of test script' 
		1 * clmTestManagementService.getTestItem(_) >> dataGenerationService.generate('/testdata/testscriptT.xml')
		
		when: w_ 'Calling method under test cacheTestItemAttachments'
		boolean success = true
		try {
			underTest.cacheTestCaseAttachments(titem)
		} catch (e) {
			success = false
		}
		
		then: t_ 'ensure success'

	}

}


@TestConfiguration
@Profile("test")
@ComponentScan(["com.zions.common.services.test"])
@PropertySource("classpath:test.properties")
class ClmTestAttachmentManagementServiceSpecConfig {
	def factory = new DetachedMockFactory()
	
	@Bean
	IGenericRestClient qmGenericRestClient() {
		return factory.Mock(ClmGenericRestClient)
	}
	
	@Bean
	ClmTestManagementService clmTestManagementService() {
		return factory.Mock(ClmTestManagementService)
	}
	
	@Bean
	ICacheManagementService cacheManagementService() {
		return factory.Mock(CacheManagementService)
	}
	
	@Bean
	ClmTestAttachmentManagementService underTest() {
		return new ClmTestAttachmentManagementService()
	}
	
	@Bean
	DataGenerationService dataGenerationService() {
		return new DataGenerationService()
	}
	
	@Bean
	TestMappingManagementService testMappingManagementService() {
		return new TestMappingManagementService()
	}

}