package com.zions.common.services.test.generators;

import com.zions.common.services.test.SpockLabeler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;

import spock.lang.Specification;
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[UuidGeneratorTestConfig])
public class UuidGeneratorTest extends Specification {

	@Autowired
	UuidGenerator underTest

	def 'gen test'() {

		when: 'call gen'
		def plan = underTest.gen()

		then: 'No exceptions'
		true
	}
}

@TestConfiguration
@Profile("test")
class UuidGeneratorTestConfig {
	def factory = new DetachedMockFactory()

	@Bean
	UuidGenerator underTest() {
		return new UuidGenerator()
	}
}
