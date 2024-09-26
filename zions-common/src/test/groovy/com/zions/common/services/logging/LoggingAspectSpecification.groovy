package com.zions.common.services.logging


import groovy.util.logging.Log
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import com.zions.common.services.test.SpockLabeler

import spock.lang.Specification

@ContextConfiguration(classes=[LoggingAspectSpecificationConfig])
class LoggingAspectSpecification extends Specification {

	@Autowired
	SomeClass someClass


	def 'Main flow for timing log'() {
		setup: 'class to be logged'

		when: 'execute something with class testing log'
		someClass.methodOne()
		someClass.methodTwo()

		then: 'validate something logged'
		true
	}

	@Autowired
	SomeClass2 someClass2

	def 'Flow for class without Slf4j annotation'() {
		setup: 'class to be logged'

		when: 'execute something with class testing log'
		someClass2.methodOne()
		someClass2.methodTwo()

		then: 'No exceptions'
		true
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

	@Bean
	SomeClass2 someClass2() {
		return new SomeClass2()
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

@Loggable


class SomeClass2 {

	def methodOne() {


		int  x=10, y=20;
		/*System.out.println("Employee class");*/
		System.out.println(x+y+" testing the aspect logging code");
	}

	def methodTwo() {

		int  x=10, y=20;
		System.out.println(x+y+" testing the apsect logging code");
		/*System.out.println("Employee class");*/
	}
}

