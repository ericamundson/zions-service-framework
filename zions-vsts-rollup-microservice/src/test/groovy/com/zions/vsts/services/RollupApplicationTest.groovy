package com.zions.vsts.services

import static org.junit.Assert.*

import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.SpockLabeler
import com.zions.vsts.services.tfs.rest.GenericRestClient
import groovyx.net.http.ContentType
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import spock.lang.Specification

@ContextConfiguration(classes=[RollupApplicationTestConfig])
class RollupApplicationTest extends Specification implements SpockLabeler {
	
	@Autowired
	IGenericRestClient genericRestClient

	public void 'Test work send'() {
//		File request = new File('src/test/resources/testdata/webrequest.json')
//		String body = request.text
//		when: w_
//		def result = genericRestClient.post(
//			contentType: ContentType.TEXT,
//			requestContentType: ContentType.JSON,
//			uri: 'http://localhost:8080/',
//			body: body
//			)
//		then: t_
//		true
	}

}

@TestConfiguration
@Profile("test")
//@ComponentScan("com.zions.vsts.services.tfs.rest")
@PropertySource("classpath:test.properties")
class RollupApplicationTestConfig {
	@Bean
	IGenericRestClient genericRestClient() {
		return new GenericRestClient('http://localhost:8080/ws', '', '')
	}
}