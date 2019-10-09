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
import com.zions.common.services.test.SpockLabeler
import spock.lang.Specification;
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[QuoteGeneratorTestConfig])
public class QuoteGeneratorTest extends Specification {
	
	
	@Autowired
	QuoteGenerator underTest
	
	@Test
	def 'init test'() {
	
		underTest.setLength(5)
		
		when: 'call init'
		def plan = underTest.init()
		
		then: 'No exceptions'
		true
	}
	
	
	@Test
	def 'gen test'() {
	
		underTest.setLength(0)
		
		when: 'call gen'
		def plan = underTest.gen()
		
		then: 'No exception'
		true
	}
	
	@Test
	def 'gen exception flow' () {
		when: 'call gen'
		def plan = underTest.gen()
		
		then: 'No exception'
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
