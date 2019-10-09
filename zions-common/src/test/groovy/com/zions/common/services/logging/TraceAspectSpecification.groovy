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

import com.zions.common.services.test.SpockLabeler

import spock.lang.Specification

@ContextConfiguration(classes=[TraceAspectSpecificationConfig])
class TraceAspectSpecification extends Specification {

	@Autowired
	SomeTraceClass someTraceClass
	
	@Autowired
	SomeTraceNoLoggerClass someTraceNoLoggerClass
	
	def 'Main flow for tracing log'() {
		setup: 'class to be logged'
		
		when: 'execute something with class testing log'
		someTraceClass.methodOne()
		someTraceClass.methodTwo()
		
		someTraceNoLoggerClass.methodOne()
		someTraceNoLoggerClass.methodTwo()
		
		then: 'No exceptions'
		true
	}

}


@TestConfiguration
@Profile("test")
@ComponentScan(["com.zions.common.services.logging"])
@PropertySource("classpath:test.properties")
class TraceAspectSpecificationConfig {
	
	@Bean
	SomeTraceClass someTraceClass() {
		return new SomeTraceClass()
	}
	
	@Bean
	SomeTraceNoLoggerClass someTraceNoLoggerClass() {
		return new SomeTraceNoLoggerClass()
	}
	
}

@Traceable
@Slf4j
class SomeTraceClass {
	
	def methodOne() {
		log.info('run methodOne')
		
	}
	
	def methodTwo() {
		log.info('run methodTwo')
	}
}

@Traceable
class SomeTraceNoLoggerClass {
	
	def methodOne() {
		println('run methodOne')
		
	}
	
	def methodTwo() {
		println('run methodTwo')
	}
}