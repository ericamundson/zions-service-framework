package com.zions.common.services.logging

import static org.junit.Assert.*

import groovy.util.logging.Slf4j
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import spock.lang.Specification

@ContextConfiguration(classes=[LoggingAspectSpecificationConfig])
class LoggingAspectSpecification extends Specification {

	@Autowired
	SomeClass someClass
	
	def 'Main flow for timing log'() {
//		setup: 'class to be logged'
//		
//		when: 'execute something with class testing log'
//		someClass.methodOne()
//		someClass.methodTwo()
//		
//		then: 'validate something logged'
//		true
	}

}


@TestConfiguration
@Profile("test")
@ComponentScan(["com.zions.common.services.logging"])
@PropertySource("classpath:test.properties")
class LoggingAspectSpecificationConfig {
	
	@Bean
	SomeClass someClass() {
		return new SomeClass()
	}
	
}

@Loggable
@Slf4j
class SomeClass {
	
	def methodOne() {
		log.info('run methodOne')
		
	}
	
	def methodTwo() {
		log.info('run methodTwo')
	}
}
