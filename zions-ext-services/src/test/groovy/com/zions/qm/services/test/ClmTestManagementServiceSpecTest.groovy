package com.zions.qm.services.test

import static org.junit.Assert.*

import com.mongodb.MongoClient
import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cache.MongoDBCacheManagementService
import com.zions.common.services.mongo.EmbeddedMongoBuilder
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.DataGenerationService

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.test.context.ContextConfiguration

import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[ClmTestManagementServiceSpecTestConfig])
class ClmTestManagementServiceSpecTest extends Specification {
	
	@Value('${clm.projectArea:}')
	String clmProject
	
	@Autowired
	IGenericRestClient qmGenericRestClient
	
	@Autowired
	ClmTestManagementService underTest
	
	@Autowired
	DataGenerationService dataGenerationService
	
	@Autowired
	ICacheManagementService cacheManagementService

	def 'getTestItem success flow.'() {
		given: g_ "A stub of RQM get test item request"
		def testcaseInfo = new XmlSlurper().parseText(this.getClass().getResource('/testdata/testcase.xml').text)
		qmGenericRestClient.get(_) >> { return testcaseInfo }
		
		when: w_ "calling of method under test (getTestItem)"
		String uri = 'https://clm.cs.zionsbank.com/qm/service/com.ibm.rqm.integration.service.IIntegrationService/resources/_aC9CQPfREeOd1div3hxkJQ/testcase/BS-82'
		def testcaseData = underTest.getTestItem(uri)
		
		then: t_ "validate test item"
		true
	}
	
	def 'getTestPlansViaQuery success flow.'() {
		given: g_ "A stub of RQM get test plans request"
		def testplansInfo = new XmlSlurper().parseText(this.getClass().getResource('/testdata/testplansquery.xml').text)
		qmGenericRestClient.get(_) >> { return testplansInfo }

		when: w_ 'calling of method under test (getTestPlansViaQuery)'
		def testPlans = underTest.getTestPlansViaQuery('', 'DigitalBanking')
		
		then: t_ 'validate list of plans'
		testPlans.entry.size() > 0
	}
	
	def 'getConfigurationsViaQuery success flow.'() {
		given: g_ "A stub of RQM get configurations request"
		def configurationsInfo = dataGenerationService.generate('/testdata/configurations.xml')
		qmGenericRestClient.get(_) >> {
			return configurationsInfo
		}

		when: w_ 'calling of method under test (getConfigurationsViaQuery)'
		def configurations = underTest.getConfigurationsViaQuery('', 'DigitalBanking')
		
		then: t_ 'validate list of configurations'
		configurations.entry.size() > 0
	}

	def 'getNextPage success flow.'() {
		given: g_ "A stub of RQM get test item request"
		def testplansInfo = new XmlSlurper().parseText(this.getClass().getResource('/testdata/nextpage.xml').text)
		qmGenericRestClient.get(_) >> {
			return testplansInfo
		}

		when: w_ 'calling of method under test (getNextPage)'
		def testPlans = underTest.nextPage('https://clm.cs.zionsbank.com/qm/service/com.ibm.rqm.integration.service.IIntegrationService/resources/Zions+FutureCore+Program+%28Quality+Management%29/testplan?token=_TJVcwOKdEeirC8bfvJTPjw&amp;page=1')
		
		then: t_ 'validate list of plans'
		testPlans.entry.size() > 0
	}
	
	def 'getExecutionResultViaHref no execution.'() {
		given: g_ "A stub of RQM get execution results request"
		def testplansInfo = new XmlSlurper().parseText(this.getClass().getResource('/testdata/executionresults.xml').text)
		qmGenericRestClient.get(_) >> {
			return testplansInfo
		}

		when: w_ 'calling of method under test (getNextPage)'
		def executionresults = underTest.getExecutionResultViaHref('123', '456', 'aproject')
		
		then: t_ 'validate list of results'
		executionresults.size() == 0
	}
	
	def 'getExecutionResultViaHref success flow.'() {
		given: g_ "A stub of RQM get execution results request"
		def testplansInfo = new XmlSlurper().parseText(this.getClass().getResource('/testdata/executionresults1.xml').text)
		qmGenericRestClient.get(_) >> {
			return testplansInfo
		}

		when: w_ 'calling of method under test (getNextPage)'
		def executionresults = underTest.getExecutionResultViaHref('123', '578', 'aproject')
		
		then: t_ 'validate list of results'
		executionresults.size() == 1
	}
	
	def 'getContent success flow.'() {
		given: g_ "A stub of RQM request to get attachment with headers"
		File file = new File('563414- Product Classifications Table')
		def of = file.newDataOutputStream()
		of.close()
		qmGenericRestClient.get(_) >> {
			return [data: of, headers: ['Content-Disposition': 'filename="563414- Product Classifications Table"']]
		}
		
		when: w_ 'Call method under test (getContent)'
		def result = underTest.getContent('http://someimage')
		
		then: t_ 'Validate result data'
		result.filename != null
	}
	
	def 'flush all queries'() {
		setup: s_ 'qm rest calls'
		cacheManagementService.cacheModule = 'QM'
		setupFlushData()
		
		when: w_ 'call flushQueries'
		underTest.flushQueries(clmProject, 2)
		
		def testCasePages = cacheManagementService.getAllOfType('TestCaseQueryData')
		
		then: t_ 'testCasePages.size() == 2'
		testCasePages.size() == 2
		
	}
	
	def setupFlushData() {
		URI uri = this.getClass().getResource('/testdata/qmflush').toURI()
		File tDir = new File(uri)
		def	pageMap = [:]
		tDir.eachFile { File page ->
			def pageInfo = dataGenerationService.generate(page)
			String d = "${pageInfo.data}"
			def data = new XmlSlurper().parseText(d)
			String url = "${pageInfo.url}"
			pageMap[url] = data
		}
		
		qmGenericRestClient.get(_) >> { args ->
			String url = "${args[0].uri}"
			return pageMap[url]
		}
	}
}

@TestConfiguration
@Profile("test")
@ComponentScan(["com.zions.common.services.test"])
@PropertySource("classpath:test.properties")
@EnableMongoRepositories(basePackages = "com.zions.common.services.cache.db")
class ClmTestManagementServiceSpecTestConfig {
	def factory = new DetachedMockFactory()
	@Value('${clm.projectArea:}')
	String clmProject

	@Bean
	IGenericRestClient qmGenericRestClient() {
		String url = 'https://clm.cs.zionsbank.com'
		String user = 'svc-rtcmigration'
		String password = 't35T1ng411rTcM!gR@t10n'
		return factory.Spy(ClmGenericRestClient, constructorArgs: [url, user, password])
	}
	
	@Bean
	ClmTestManagementService underTest() {
		return new ClmTestManagementService()
	}
	
	@Bean
	TestMappingManagementService testMappingManagementService() {
		return new TestMappingManagementService()
	}
	
	@Bean
	DataGenerationService dataGenerationService() {
		return new DataGenerationService()
	}
	
	@Bean
	ICacheManagementService cacheManagementService() {
		//return new CacheManagementService(cacheLocation)
		return new MongoDBCacheManagementService()
	}

	@Value('${spring.data.mongodb.database:adomigration_dev}')
	String database

	@Bean
	MongoClient mongoClient() throws UnknownHostException {
		//Logger.getLogger(Loggers.PREFIX).setLevel(Level.OFF);
		def builder = new EmbeddedMongoBuilder()
			.version('3.2.16')
			//.tempDir('mongodb')
			.installPath('../zions-common-data/mongodb/win32/mongodb-win32-x86_64-3.2.16/bin')
			.bindIp("localhost")
			.port(12346)
			.build();
		return builder
	}
	
	public @Bean MongoTemplate mongoTemplate() throws Exception {
		return new MongoTemplate(mongoClient(), database);
	}

}


