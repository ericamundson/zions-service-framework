package com.zions.vsts.services.work

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.SpockLabeler
import com.zions.vsts.services.tfs.rest.GenericRestClient
import groovy.json.JsonSlurper
import groovyx.net.http.RESTClient
import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[FileManagementServiceSpecConfig])
class FileManagementServiceSpec extends Specification implements SpockLabeler {
	
	@Autowired
	IGenericRestClient genericRestClient
		
	@Autowired
	FileManagementService underTest
	
	@Autowired
	ICacheManagementService cacheManagementService

	def 'ensureAttachments main flow'() {
		given: g_ 'stub for WorkManagementService getCacheWI call'
		def wiData = new JsonSlurper().parseText(this.getClass().getResource('/testdata/cacheworkitem.json').text)
		1 * cacheManagementService.getFromCache(_,_) >> wiData
		
		and: a_ 'stub for upload attachment rest request'
		1 * genericRestClient.rateLimitPost(_, _) >> [url: 'https://an.azure.location']
		
		when: w_ 'call method under test ensureAttachments'
		def wiUpdate = underTest.ensureAttachments('', '', 'aId', [[file: new File('dumb.png'), comment: "Added dumb.png"]])
		
		then: t_ 'a valid work item change request must be returned.'
		wiUpdate.body.size() > 1
	}

}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class FileManagementServiceSpecConfig {
	def mockFactory = new DetachedMockFactory()
	@Autowired
	@Value('${cache.location}')
	String cacheLocation

	@Bean
	IGenericRestClient genericRestClient() {
		RESTClient delegate = new RESTClient()
		IGenericRestClient out = mockFactory.Spy(GenericRestClient, constructorArgs: [delegate])
		return out
	}
	
	@Bean
	FileManagementService underTest() {
		FileManagementService out = new FileManagementService()
		return out
	}
	
	@Bean
	ICacheManagementService cacheManagementService() {
		return mockFactory.Mock(CacheManagementService)
	}


}