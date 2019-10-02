package com.zions.clm.services.ccm.workitem

import static org.junit.Assert.*

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

import com.mongodb.MongoClient
import com.zions.clm.services.ccm.client.CcmGenericRestClient
import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.clm.services.rtc.project.workitems.ClmWorkItemManagementService
import com.zions.common.services.attachments.IAttachments
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cache.MongoDBCacheManagementService
import com.zions.common.services.mongo.EmbeddedMongoBuilder
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.DataGenerationService
import com.zions.common.services.test.SpockLabeler
import spock.lang.Specification
import spock.mock.DetachedMockFactory


@ContextConfiguration(classes=[CcmWorkManagementServiceSpecConfig])
class CcmWorkManagementServiceSpec extends Specification implements SpockLabeler {
	
	@Autowired
	IGenericRestClient genericRestClient
	
	@Autowired
	IAttachments attachmentService
	
	@Autowired
	CcmWorkManagementService underTest
	
	@Autowired
	DataGenerationService dataGenerationService
	
	@Value('${clm.projectArea}')
	String clmProjectArea


	def 'Get ADO work item changes from CCM work items'() {
		setup: s_ 'a set of ccm work items of various types'
		genericRestClient.get(_) >> {
			return null
		}		
		attachmentService.sendAttachment(_) >> {}
		def linkMapping = getLinkMapping()
		def membersMap = dataGenerationService.generate('/testdata/getProjectMembersMap.json')
		def translateMap = dataGenerationService.generate('/testdata/getTranslateMapping.json')
		def queryData = dataGenerationService.generate('/testdata/mbworkitemquery.xml')
		
		when: w_ 'get ADO changes for CCM work items'
		def adoWIChangeList = []
		queryData.workItem.each { wi ->
			String sid = "${wi.id.text()}"
			def changes = underTest.getWIChanges(sid, clmProjectArea, translateMap, membersMap)
			adoWIChangeList.add(changes)
			
		}
		
		then: t_ 'validate set of ADO changes'
		true
	}
	
	def 'Get ADO work item link changes from CCM work item link relationships'() {
		setup: s_ 'a set of ccm work items of various types'
		genericRestClient.get(_) >> {
			return null
		}
		attachmentService.sendAttachment(_) >> {}
		def linkMapping = getLinkMapping()
		def membersMap = dataGenerationService.generate('/testdata/getProjectMembersMap.json')
		def translateMap = dataGenerationService.generate('/testdata/getTranslateMapping.json')
		def queryData = dataGenerationService.generate('/testdata/mbworkitemquery.xml')

		when: w_ 'get ADO link changes for CCM work items'
		queryData.workItem.each { wi ->
			String sid = "${wi.id.text()}"
			def changes = underTest.getWIChanges(sid, clmProjectArea, translateMap, membersMap)
			
		}
		def adoWIChangeList = []
		queryData.workItem.each { wi ->
			String sid = "${wi.id.text()}"
			int id = Integer.parseInt(sid)
			underTest.getWILinkChanges(id, clmProjectArea, linkMapping) { type, changes ->
				if (type == 'WorkItem') {
					adoWIChangeList.add(changes)
				}
				
			}
			
		}

		then: t_ 'validate set of ADO link changes'
		true
	}
	
	def setupWorkitemStubForChanges() {
		genericRestClient.get(_) >> {
			return null
		}
		
		attachmentService.sendAttachment(_) >> {}
		
	}
	
	def getLinkMapping() {
		def mapping = dataGenerationService.generate('/testdata/MBWITMapping.xml')
		def ilinkMapping = [:]
		mapping.links.link.each { link ->
			ilinkMapping["${link.@source}"] = link
		}
		return ilinkMapping
	}


}

@TestConfiguration
@Profile("integration-test")
@ComponentScan(["com.zions.clm.services","com.zions.clm.services.ccm", "com.zions.common.services.test.generators"])
@PropertySource("classpath:integration-test.properties")
@EnableMongoRepositories(basePackages = "com.zions.common.services.cache.db")
class CcmWorkManagementServiceSpecConfig {
	def factory = new DetachedMockFactory()
	
	@Value('${clm.url}') 
	String clmUrl
	
	@Value('${clm.user}') 
	String userid
	
	@Value('${clm.password}') 
	String password
	
	@Bean
	DataGenerationService dataGenerationService() {
		return new DataGenerationService()
	}

	
	@Bean
	IAttachments attachmentService() {
		return factory.Stub(IAttachments)
	}
	
	@Bean
	IGenericRestClient genericRestClient() {
		return factory.Stub(IGenericRestClient)
	}
	
	@Bean
	CcmWorkManagementService underTest()
	{
		return new CcmWorkManagementService()
	}
	
//	ClmWorkItemManagementService clmWorkItemManagementService() {
//		return new ClmWorkItemManagementService()
//	}
//	@Bean
//	CcmGenericRestClient ccmGenericRestClient()
//	{
//		return new CcmGenericRestClient()
//	}
//	IGenericRestClient clmGenericRestClient() {
//		return new ClmGenericRestClient(clmUrl, userid, password)
//	}
	
	
	@Bean
	ICacheManagementService cacheManagementService() {
		//return new CacheManagementService(cacheLocation)
		return new MongoDBCacheManagementService()
	}
	
	@Value('${spring.data.mongodb.database:adomigration_dev}')
	String database
	
	@Bean
	MongoClient mongoClient() throws UnknownHostException {
		def builder = new EmbeddedMongoBuilder()
			.version('3.2.16')
			//.tempDir('mongodb')
			.installPath('../zions-common-data/mongodb/win32/mongodb-win32-x86_64-3.2.16/bin')
			.bindIp("localhost")
			.port(12345)
			.build();
		//Logger.getLogger(Loggers.PREFIX).setLevel(Level.SEVERE);
		return builder
	}
	
	public @Bean MongoTemplate mongoTemplate() throws Exception {
		return new MongoTemplate(mongoClient(), database);
	}


}
