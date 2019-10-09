package com.zions.common.services.test.generators;

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;

import com.zions.common.services.test.SpockLabeler

import spock.lang.Specification;
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[IntegerGeneratorTestConfig])
public class IntegerGeneratorTest extends Specification {

	@Autowired
	IntegerGenerator underTest

	@Test
	def 'gen test'() {

		when: 'call gen'
		def plan = underTest.gen()

		then: 'No exceptions'
		true
	}
}

@TestConfiguration
@Profile("test")
class IntegerGeneratorTestConfig {
	def factory = new DetachedMockFactory()

	@Bean
	IntegerGenerator underTest() {
		return new IntegerGenerator()
	}
}

