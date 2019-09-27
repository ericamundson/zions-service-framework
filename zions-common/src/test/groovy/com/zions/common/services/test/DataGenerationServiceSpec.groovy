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
		given: s_ 'setup file'
		File template = new File(this.getClass().getResource('/testdata/TestPlanT.json').file)
		
		when: w_ 'call generate'
		def plan = underTest.generate(template)
		
		then: t_ 'No exceptions'
		true
	}
	
	@Test
	def 'generate string resouce test with json' () {
		given: g_ 'setup data template'
		def resourceJson = '/testdata/TestPlanT.json'
		when: w_ 'call generate'
		def plan = underTest.generate(resourceJson)
		then: t_ 'plan != null'
		plan != null
	}
	
	@Test
	def 'generate string resouce test with xml' () {
		given: g_ 'setup data template'
		def resourceXml= '/testdata/testcase.xml'
		when: w_ 'call generate'
		def plan = underTest.generate(resourceXml)
		then: t_ 'No exceptions'
		true
	}
	
	@Test
	def 'generate test with url' () {
		given: 'json data to do replacement'
		def resourceJson = '/testdata/TestPlanT.json'
		def resourceXml= '/testdata/testcase.xml'
		
		URL urlJson = this.getClass().getResource(resourceJson)
		URL urlXml = this.getClass().getResource(resourceXml)
		
		when: w_ 'call generate'
		def planJSON = underTest.generate(urlJson)
		def planXML = underTest.generate(urlXml)
		
		then: t_ 'planJSON != null && planXML != null'
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