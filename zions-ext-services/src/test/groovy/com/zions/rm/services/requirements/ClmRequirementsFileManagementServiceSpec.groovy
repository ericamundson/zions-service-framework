package com.zions.rm.services.requirements

import static org.junit.Assert.*
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.common.services.test.DataGenerationService
import com.zions.qm.services.test.ClmTestAttachmentManagementService
import com.zions.qm.services.test.ClmTestAttachmentManagementServiceSpecConfig
import com.zions.qm.services.test.ClmTestManagementService
import com.zions.qm.services.test.TestMappingManagementService
import com.zions.rm.services.requirements.RequirementsMappingManagementService
import com.zions.rm.services.requirements.ClmModuleElement
import com.zions.rm.services.requirements.ClmRequirementsFileManagementService
import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[ClmRequirementsFileManagementServiceSpecConfig])
class ClmRequirementsFileManagementServiceSpec extends Specification  {
	@Autowired
	IGenericRestClient rmGenericRestClient

	@Autowired
	ClmRequirementsManagementService clmRequirementsManagementService

	@Autowired
	ICacheManagementService cacheManagementService
	
	@Autowired
	RequirementsMappingManagementService requirementsMappingManagementService
	
	@Autowired
	ClmRequirementsFileManagementService underTest
	
	def 'requirement file attachment main flow'() {
		given: 'Module Element with wrapped resource file format'
		String wrappedResourceFilename = 'stuff.txt'
		def ritem = new ClmModuleElement(wrappedResourceFilename, 'https://...', 1, 'Wrapped Resource', 'false', 'https://...')
		ritem.setID('123456')
		ritem.setTfsWorkitemType('User Story')
		
		and: 'Step of cacheManagementService saveBinaryAsAttachment call'
		File attFile = new File('attachment.txt')
		1 * cacheManagementService.saveBinaryAsAttachment(_,_,_) >> attFile
		
		and: 'Stub of clmRequirementsManagementService getContent call'
		String encodedStuff = "Here's some text".bytes.encodeBase64()
		ByteArrayInputStream s = new ByteArrayInputStream("Here's some text".bytes)
		1 * clmRequirementsManagementService.getContent(_) >> [headers: ['Content-Disposition': 'filename=\"stuff.txt\";'], data: s]
		
		when: 'Calling method under test cacheRequirementFile'
		boolean success = true
		try {
			underTest.cacheRequirementFile(ritem)
		} catch (e) {
			success = false
		}
		
		then: 'ensure success'
	}

}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class ClmRequirementsFileManagementServiceSpecConfig {
	def factory = new DetachedMockFactory()
	
	@Bean
	IGenericRestClient rmGenericRestClient() {
		return factory.Mock(ClmGenericRestClient)
	}
	
	@Bean
	ClmRequirementsManagementService clmRequirementsManagementService() {
		return factory.Mock(ClmRequirementsManagementService)
	}
	
	@Bean
	ICacheManagementService cacheManagementService() {
		return factory.Mock(CacheManagementService)
	}
	
	@Bean
	ClmRequirementsFileManagementService underTest() {
		return new ClmRequirementsFileManagementService()
	}
	
	@Bean
	RequirementsMappingManagementService rmMappingManagementService() {
		return new RequirementsMappingManagementService()
	}

}