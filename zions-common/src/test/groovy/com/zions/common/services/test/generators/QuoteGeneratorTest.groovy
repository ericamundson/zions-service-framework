package com.zions.common.services.test.generators;

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;

import com.zions.common.services.cache.CacheManagementService;
import com.zions.common.services.cache.CacheManagementServiceTestConfig;

import spock.lang.Specification;
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[QuoteGeneratorTestConfig])
public class QuoteGeneratorTest extends Specification {
	
	
	@Autowired
	QuoteGenerator underTest
	
	@Test
	def 'init test'() {
	
		underTest.setLength(5)
		
		when: w_ 'call init'
		def plan = underTest.init()
		
		then: t_ 'No exceptions'
		true
	}
	
	
	@Test
	def 'gen test'() {
	
		underTest.setLength(0)
		
		when: w_ 'call gen'
		def plan = underTest.gen()
		
		then: t_ null
		true
	}
	
	@Test
	def 'gen exception flow' () {
		when: w_ 'call gen'
		def plan = underTest.gen()
		
		then: t_ null
		true
		
	}
	
}

@TestConfiguration
@Profile("test")
class QuoteGeneratorTestConfig {
	def factory = new DetachedMockFactory()
	
		
	@Bean
	QuoteGenerator underTest() {
		return new QuoteGenerator()
	}
}
