package com.zions.qm.services.cli.action.test

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration
import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.SpockLabeler
import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[TranslateRQMToMTMSpecSpecConfig])
class TranslateRQMToMTMSpec extends Specification {

	def "Full run"() {
		
	}

}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class TranslateRQMToMTMSpecSpecConfig {
	def factory = new DetachedMockFactory()
	
	@Bean
	IGenericRestClient qmGenericRestClient() {
		return factory.Mock(ClmGenericRestClient)
	}
	
	@Bean
	TranslateRQMToMTM underTest() {
		return new TranslateRQMToMTM()
	}

}