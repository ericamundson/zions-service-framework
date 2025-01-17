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

@ContextConfiguration(classes=[DateGeneratorTestConfig])
public class DateGeneratorTest extends Specification {
	
	@Autowired
	DateGenerator underTest
	
	def 'gen test'() {
	
//		when:
//		def plan = underTest.gen()
//		
//		then:
//		true
	
	}

}


@TestConfiguration
@Profile("test")
class DateGeneratorTestConfig {
	def factory = new DetachedMockFactory()
	
	@Bean
	DateGenerator underTest() {
		return new DateGenerator()
	}
}
