package com.zions.common.services.test

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import spock.lang.Specification

@ContextConfiguration(classes=[DataGenerationServiceSpecConfig])
class DataGenerationServiceSpec extends Specification {
	@Autowired
	DataGenerationService underTest

	def 'generateForJson test'() {
		given: 'setup file'
		File template = new File(this.getClass().getResource('/testdata/TestPlanT.json').file)
		
		when:
		def plan = underTest.generate(template)
		
		then:
		true
	}

}


@TestConfiguration
@ComponentScan(["com.zions.common.services.test"])
class DataGenerationServiceSpecConfig {
	
	@Bean
	DataGenerationService underTest() {
		return new DataGenerationService()
	}
	
}