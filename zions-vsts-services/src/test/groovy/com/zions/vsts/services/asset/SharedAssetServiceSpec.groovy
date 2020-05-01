package com.zions.vsts.services.asset

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

@ContextConfiguration(classes=[SharedAssetServiceSpecConfig])
class SharedAssetServiceSpec extends Specification {
	
	@Autowired
	IGenericRestClient genericRestClient
		
	@Autowired
	SharedAssetService underTest

	def 'getAsset main flow'() {
		true
	}

}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class SharedAssetServiceSpecConfig {
	def mockFactory = new DetachedMockFactory()
	@Autowired
	@Value('${cache.location}')
	String cacheLocation

	@Bean
	IGenericRestClient genericRestClient() {
		return mockFactory.Mock(GenericRestClient, name: 'genericRestClient')
	}
	
	@Bean
	SharedAssetService underTest() {
		SharedAssetService out = new SharedAssetService()
		return out
	}
	
	@Bean
	ICacheManagementService cacheManagementService() {
		return mockFactory.Mock(CacheManagementService)
	}


}