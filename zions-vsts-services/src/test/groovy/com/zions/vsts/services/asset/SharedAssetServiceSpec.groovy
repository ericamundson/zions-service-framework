package com.zions.vsts.services.asset




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

	@Value('${tfs.collection}')
	String collection

	@Value('${tfs.colorMapUID:}')
	String colorMapUID

	def 'getAsset main flow'() {
		given: "A stub REST Client"
		def adoMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/colormap.json').text)
		genericRestClient.get(_) >> adoMap

		when: "Get colormap asset from ADO Control data store"
		underTest.getAsset(collection, colorMapUID)

		then: "SharedAssetService has one asset cached"
		underTest.assetMap.size() == 1
		
		and: "Asset matches expected colormap"
		underTest.assetMap[colorMapUID] == adoMap.value
	}

}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class SharedAssetServiceSpecConfig {
	def mockFactory = new DetachedMockFactory()
	@Autowired
	@Value('${tfs.collection}')
	String collection

	@Autowired
	@Value('${tfs.colorMapUID:}')
	String colorMapUID

	@Bean
	IGenericRestClient genericRestClient() {
		return mockFactory.Stub(GenericRestClient, name: 'genericRestClient')
	}
	
	@Bean
	SharedAssetService underTest() {
		SharedAssetService out = new SharedAssetService()
		return out
	}


}