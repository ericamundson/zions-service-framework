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
	
	@Test
	def 'generate string resouce test with json' () {
		given:
		def resourceJson = '/testdata/TestPlanT.json'
		when:
		def plan = underTest.generate(resourceJson)
		then:
		plan != null
	}
	
	@Test
	def 'generate string resouce test with xml' () {
		given:
		def resourceXml= '/testdata/testcase.xml'
		when:
		def plan = underTest.generate(resourceXml)
		then:
		true
	}
	
	@Test
	def 'generate test with url' () {
		given:
		def resourceJson = '/testdata/TestPlanT.json'
		def resourceXml= '/testdata/testcase.xml'
		
		URL urlJson = this.getClass().getResource(resourceJson)
		URL urlXml = this.getClass().getResource(resourceXml)
		
		when:
		def planJSON = underTest.generate(urlJson)
		def planXML = underTest.generate(urlXml)
		
		then:
		planJSON != null
		planXML != null
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