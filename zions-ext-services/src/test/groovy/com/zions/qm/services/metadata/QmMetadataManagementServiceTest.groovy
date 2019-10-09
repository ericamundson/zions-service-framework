package com.zions.qm.services.metadata;

import static org.junit.Assert.*

import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.qm.services.project.QmProjectManagementService
import com.zions.qm.services.test.ClmTestManagementService
import groovy.json.JsonSlurper
import groovy.xml.MarkupBuilder
import com.zions.bb.services.code.BBCodeManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.SpockLabeler
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[QmMetadataManagementServiceTestConfig])
public class QmMetadataManagementServiceTest extends Specification {
	
	@Autowired
	IGenericRestClient qmGenericRestClient
	
	@Autowired
	QmProjectManagementService  projectService
	
	@Autowired
	QmMetadataManagementService underTest
	
	def 'loadSchema success flow.'() {
		given: "A stub of RQM get test item request"
		def testplansInfo = new XmlSlurper().parseText(this.getClass().getResource('/testdata/qmmetadata.xml').text)
		1 * qmGenericRestClient.get(_) >> testplansInfo

		when: 'calling of method under test (loadSchema)'
		def testPlans = underTest.loadSchema('','')
		
		then: 'No exception'
		true
	}
	
	def 'determineType success flow.'() {
				
		//String typeName = 'xs:boolean'
		String typeName = 'qm:richTextSection'
		def testplansInfo = new XmlSlurper().parseText(this.getClass().getResource('/testdata/qmmetadata2.xml').text)
		1 * qmGenericRestClient.get(_) >> testplansInfo
		when: 'call determineType'
		def testPlans = underTest.determineType( testplansInfo , typeName)
		
		then: 'No exception'
		true
	}
	
	def 'generateCustomFields success flow.'() {
		given: "A stub of RQM get test item request"		
		def testplan = new JsonSlurper().parseText(getClass().getResource('/testdata/categoryrepo.json').text)
		1 * qmGenericRestClient.get(_) >> testplan
		
		def test = new JsonSlurper().parseText(getClass().getResource('/testdata/projectrepo2.json').text)
		1 * qmGenericRestClient.get(_) >> test
		
		def xmlWriter = new StringWriter()
		def xmlMarkup = new MarkupBuilder(xmlWriter)

		when: 'calling of method under test (generateCustomFields)'
		def testPlans = underTest.generateCustomFields('projectArea','TestCase', xmlMarkup )
		
		then: 'No exception'
		true
	}
	
	def 'getQMSchema success flow.'() {
		given: "A stub of RQM get test item request"
		def testplansInfo = new XmlSlurper().parseText(this.getClass().getResource('/testdata/qmmetadata.xml').text)
		1 * qmGenericRestClient.get(_) >> testplansInfo

		when: 'calling of method under test (getQMSchema)'
		def testPlans = underTest.getQMSchema()
		
		then: 'No exception'
		true
	}
		
	def 'findSchema success flow.'() {
		
		def testplansInfo = new XmlSlurper().parseText(this.getClass().getResource('/testdata/schema.xml').text)
		
		when: 'call findSchema'
		def testPlans = underTest.findSchema( testplansInfo ,'testcase')
		
		then: 'No exception'
		true
	}
	
	def 'extractQmMetadata success flow.'() {
		
		//def testplansInfo = new XmlSlurper().parseText(this.getClass().getResource('/testdata/qmmetadata.xml').text)
		File file = new File("src/test/resources/testdata")
		
		given: "A stub of RQM get test item request"
		def testplansInfo = new XmlSlurper().parseText(this.getClass().getResource('/testdata/qmmetadata.xml').text)
		1 * qmGenericRestClient.get(_) >> testplansInfo
		
		def testplans = new XmlSlurper().parseText(this.getClass().getResource('/testdata/schema.xml').text)
		
		when: 'call extractQmMetadata'
		def testPlans = underTest.extractQmMetadata( 'projectArea' , file )
		
		then: 'No exception'
		true
	}
	
	def 'generateComplexFields success flow.'() {
		
		given: "A stub of RQM get test item request"
		def testplansInfo = new XmlSlurper().parseText(this.getClass().getResource('/testdata/qmmetadata2.xml').text)
		def complexType = new XmlSlurper().parseText(this.getClass().getResource('/testdata/qmmetadata3.xml').text)
		
		def xmlWriter = new StringWriter()
		def xmlMarkup = new MarkupBuilder(xmlWriter)
		when: 'call generateComplexFields'
		def testPlans = underTest.generateComplexFields( 'projectArea','testplan', testplansInfo , complexType , xmlMarkup )
		
		then: 'No exception'
		true
	}
	
	def 'generateCategoryFields success flow.'() {
		
		given: "A stub of RQM get test item request"
		def testplan = new JsonSlurper().parseText(getClass().getResource('/testdata/projectrepo.json').text)
		1 * qmGenericRestClient.get(_) >> testplan
		
		def test = new JsonSlurper().parseText(getClass().getResource('/testdata/projectrepo2.json').text)
		1 * qmGenericRestClient.get(_) >> test
		
		def xmlWriter = new StringWriter()
		def xmlMarkup = new MarkupBuilder(xmlWriter)
		
		when: 'calling of method under test (getCategories)'
		def testPlans = underTest.generateCategoryFields('projectArea','testplan', xmlMarkup )
		
		then: 'No exception'
		true
	}

}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class QmMetadataManagementServiceTestConfig {
	def factory = new DetachedMockFactory()
	
	@Bean
	IGenericRestClient qmGenericRestClient() {
		return factory.Mock(ClmGenericRestClient)
	}
	
	@Bean
	QmMetadataManagementService underTest() {
		return new QmMetadataManagementService()
	}
	
	@Bean
	QmProjectManagementService projectService() {
		return new QmProjectManagementService()
	}
}
