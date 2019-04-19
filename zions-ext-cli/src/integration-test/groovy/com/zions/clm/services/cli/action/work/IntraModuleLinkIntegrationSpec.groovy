package com.zions.clm.services.cli.action.work

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.ContextConfiguration

import com.mongodb.MongoClient
import com.zions.clm.services.ccm.client.RtcRepositoryClient
import com.zions.clm.services.ccm.workitem.CcmWorkManagementService
import com.zions.clm.services.ccm.workitem.WorkitemAttributeManager
import com.zions.clm.services.ccm.workitem.attachments.AttachmentsManagementService
import com.zions.clm.services.ccm.workitem.metadata.CcmWIMetadataManagementService
import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.clm.services.rtc.project.workitems.ClmWorkItemManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cache.MongoDBCacheManagementService
import com.zions.common.services.mongo.EmbeddedMongoBuilder
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.DataGenerationService
import com.zions.common.services.work.handler.IFieldHandler
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.work.FileManagementService
import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.work.templates.ProcessTemplateService

import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[IntraModuleLinkIntegrationSpecConfig])
class IntraModuleLinkIntegrationSpec extends Specification {

	def 'CCM to QM item linking'() {
		given: 'Setup of RQM to ADO test elements'
		setupRQMToADO()
		
		and: 'Setup of CCM to ADO work item elements'
		and: 'Setup of ccm link data'
		
		when: 'When CCM link phase called'
		then: 'Then validate intra module links'
		true
	}
	
	def setupRQMToADO()
	{
		
	}
	
	private String[] loadCCMArgs(def mappingFile) {
		String[] args = [
			'--clm.url=https://clm.cs.zionsbank.com',
			'--clm.user=z091182',
			'--clm.projectArea=src',
			'--ccm.template.dir=./src/test/resources/testdata.wit_templates',
			'--tfs.url=https://dev.azure.com/eto-dev',
			'--tfs.user=z091182',
			'--tfs.project=tfsproject',
			'--wit.mapping.file=' + mappingFile,
			'--wi.query=query',
			'--wi.filter=allfilter',
			'--include.update=phases',
			'--meta=meta',
			'--refresh=true',
			"--tfs.collection=''"
		]
		return args
	}

}

@TestConfiguration
@Profile("test")
@ComponentScan(["com.zions.vsts.services", "com.zions.common.services.restart", "com.zions.common.services.cacheaspect"])
@PropertySource("classpath:test.properties")
class IntraModuleLinkIntegrationSpecConfig {
	def mockFactory = new DetachedMockFactory()
	
	@Bean
	RtcRepositoryClient rtcRepositoryClient() {
		return mockFactory.Mock(RtcRepositoryClient)
	}
	
	@Bean
	CcmWIMetadataManagementService ccmWIMetadataManagementService() {
		//return new CcmWIMetadataManagementService()
		return mockFactory.Mock(CcmWIMetadataManagementService)
	}
	
	@Bean
	IGenericRestClient clmGenericRestClient() {
		//ClmGenericRestClient c
		return mockFactory.Mock(ClmGenericRestClient, name: 'clmGenericRestClient')
	}
	
	@Bean
	ClmWorkItemManagementService clmWorkItemManagementService() {
		//return new ClmWorkItemManagementService()
		return mockFactory.Mock(ClmWorkItemManagementService)
	}
	
	@Bean
	Map<String, IFieldHandler> fieldMap() {
		return ['filter':iFilter()]
	}
	
	@Bean
	WorkitemAttributeManager workitemAttributeManager() {
		return mockFactory.Mock(WorkitemAttributeManager)
	}
	
	@Bean
	CcmWorkManagementService ccmWorkManagementService() {
		return mockFactory.Mock(CcmWorkManagementService)
	}
	
	
	@Bean
	AttachmentsManagementService attachmentsManagementService() {
		return mockFactory.Mock(AttachmentsManagementService)
	}
	
	
	@Bean
	TranslateRTCWorkToVSTSWork underTest() {
		return new TranslateRTCWorkToVSTSWork()
	}

	@Bean
	DataGenerationService dataGenerationService() {
		return new DataGenerationService()
	}
	
	public MongoClient mongoClient() throws Exception {
		
		return new EmbeddedMongoBuilder()
			.version('3.2.16')
			.downloadPath('file:./../zions-common-data/mongodb/')
			.bindIp("127.0.0.1")
			.port(12345)
			.build();
	}

	
	public @Bean MongoTemplate mongoTemplate() throws Exception {
		return new MongoTemplate(mongoClient(), getDatabaseName());
	}

	protected String getDatabaseName() {
		// TODO Auto-generated method stub
		return 'coredev';
	}
	
	@Bean
	ICacheManagementService cacheManagementService() {
		return new MongoDBCacheManagementService()
	}
}