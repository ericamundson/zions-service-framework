package com.zions.clm.services.cli.action.work

import static org.junit.Assert.*

import java.awt.image.ImageFilter

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ContextConfiguration

import com.zions.clm.services.ccm.client.RtcRepositoryClient
import  com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.clm.services.ccm.workitem.CcmWorkManagementService
import com.zions.clm.services.ccm.workitem.WorkitemAttributeManager
import com.zions.clm.services.ccm.workitem.attachments.AttachmentsManagementService
import com.zions.clm.services.ccm.workitem.metadata.CcmWIMetadataManagementService
import com.zions.clm.services.rtc.project.workitems.ClmWorkItemManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.query.IFilter
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.work.handler.IFieldHandler
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.work.FileManagementService
import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.work.templates.ProcessTemplateService
import groovy.json.JsonSlurper
import com.zions.vsts.services.tfs.rest.GenericRestClient

import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[TranslateRTCWorkToVSTSWorkSTestConfig])
class TranslateRTCWorkToVSTSWorkSpecTest extends Specification {
	
	@Autowired
	private Map<String, IFilter> filterMap
	
	@Autowired
	AllFilter allFilter
	
	@Autowired
	RtcRepositoryClient rtcRepositoryClient
	
	@Autowired
	CcmWIMetadataManagementService ccmWIMetadataManagementService
	
	@Autowired
	IGenericRestClient genericRestClient
	
	@Autowired(required=true)
	ProjectManagementService projectManagementService;
	
	@Autowired
	ProcessTemplateService processTemplateService
	
	@Autowired
	ICacheManagementService cacheManagementService
	
	@Autowired
	WorkManagementService workManagementService
	
	@Autowired
	IGenericRestClient clmGenericRestClient
	
	@Autowired
	ClmWorkItemManagementService clmWorkItemManagementService
	
	@Autowired
	Map<String, IFieldHandler> fieldMap
	
	@Autowired
	WorkitemAttributeManager workitemAttributeManager
	
	@Autowired
	CcmWorkManagementService ccmWorkManagementService
	
	@Autowired
	MemberManagementService memberManagementService
	
	@Autowired
	AttachmentsManagementService attachmentsManagementService
	
	@Autowired
	FileManagementService fileManagementService

	@Autowired
	TranslateRTCWorkToVSTSWork underTest
	
	private String[] loadArgs(def mappingFile) {
		String[] args = [
			'--clm.url=http://localhost:8080',
			'--clm.user=user',
			'--clm.projectArea=src',
			'--ccm.template.dir=./src/test/resources/testdata.wit_templates',
			'--tfs.url=tfsUrl',
			'--tfs.user=tfsUser',
			'--tfs.project=tfsproject',
			'--wit.mapping.file=' + mappingFile,
			'--wi.query=query',
			'--wi.filter=allfilter',
			'--include.update=meta,refresh,workdata,worklinks,attachments',
			'--meta=meta',
			'--refresh=true',
			'--tfs.collection=tfsCollection'
		]
		return args
	}
	
	@Test
	def 'validate ApplicationArguments success flow.'() {
		given: 'Stub with Application Arguments'
		def appArgs = new DefaultApplicationArguments(loadArgs('./src/test/resources/testdata/OBWITMapping.xml'))


		when: 'calling of method under test (validate)'
		def result = underTest.validate(appArgs)

		then: ''
		result == true
	}
	
	@Test
	def 'validate ApplicationArguments exception flow.'() {
		given:'Stub with Application Arguments'
		String[] args = ['--clm.url=http://localhost:8080']
		def appArgs = new DefaultApplicationArguments(args)
		
		when: 'calling of method under test (validate)'
		def result = underTest.validate(appArgs)
		
		then:
		thrown Exception
	}
	
	@Test
	def 'execute ApplicationArguments exception flow.' () {
		given: 'Stub with Application Arguments'
		String[] args = loadArgs()
		def appArgs = new DefaultApplicationArguments(args)
		
		when: 'calling of method under test (validate)'
		def result = underTest.execute(appArgs)

		then: ''
		thrown FileNotFoundException
	}
	
	@Test
	def 'execute ApplicationArguments Exception flow.' () {
		given: 'Stub with Application Arguments'
		def appArgs = new DefaultApplicationArguments(loadArgs('./src/test/resources/testdata/OBWITMapping.xml'))
		
		and:
		def workItems = new XmlSlurper().parse(new File('./src/test/resources/testdata/workitems.xml'))
		processTemplateService.updateWorkitemTemplates(_, _, _, _) >> workItems
		
		and:
		def wititems = new XmlSlurper().parse(new File('./src/test/resources/testdata/workitems.xml'))
		clmWorkItemManagementService.getWorkItemsViaQuery(_) >> wititems
		
		and:
		def wiwChanges  = [value: ["{\"id\":\"123\", \"somejson\": \"morejson\"}"]]
		workManagementService.refreshCache(_, _, _) >> wiwChanges
		
		and:
		def translateMapping = new JsonSlurper().parseText(this.getClass().getResource('/testdata/processfields.json').text)
		processTemplateService.getTranslateMapping(_, _, _, _)
		
		and:
		def memberMap = new JsonSlurper().parseText(this.getClass().getResource('/testdata/teammembers.json').text)
		memberManagementService.getProjectMembersMap(_, _) >> memberMap
		
		and:
		def wicChanges  = [value: ["{\"id\":\"123\", \"somejson\": \"morejson\"}"]]
		ccmWorkManagementService.getWIChanges(_, _, _, _) >> wicChanges
		
		and:
		def files = [new File('text1.txt'), new File('text1.txt')]
		attachmentsManagementService.cacheWorkItemAttachments(_) >> files
		
		and:
		def idMap = [source:"Default", value:"Something"]
		def wiChanges = []
		wiChanges.add(idMap)
		fileManagementService.ensureAttachments(_, _, _, _) >> wiChanges
		
		when: 'calling of method under test (validate)'
		def result = underTest.execute(appArgs)

		then: ''
		thrown NullPointerException
	}
	
}

@TestConfiguration
@Profile("test")
@PropertySource("classpath:test.properties")
class TranslateRTCWorkToVSTSWorkSTestConfig {
	def mockFactory = new DetachedMockFactory()
	
	@Bean
	IFilter iFilter() {
		return mockFactory.Mock(AllFilter)
	}
	
	@Bean
	Map<String, IFilter> filterMap() {
		return ['filter':iFilter()]
	}
	
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
	IGenericRestClient genericRestClient() {
		return mockFactory.Mock(GenericRestClient, name: 'genericRestClient')
	}
	
	@Bean
	ProjectManagementService projectManagementService() {
		return new ProjectManagementService()
	}
	
	@Bean
	ProcessTemplateService processTemplateService() {
		//return new ProcessTemplateService()
		return mockFactory.Mock(ProcessTemplateService)
	}
	
	@Bean
	ICacheManagementService cacheManagementService() {
		return mockFactory.Mock(ICacheManagementService)
	}
	
	@Bean
	WorkManagementService workManagementService() {
		return new WorkManagementService()
	}
	
	@Bean
	IGenericRestClient clmGenericRestClient() {
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
	MemberManagementService memberManagementService() {
		return mockFactory.Mock(MemberManagementService)
	}
	
	@Bean
	AttachmentsManagementService attachmentsManagementService() {
		return mockFactory.Mock(AttachmentsManagementService)
	}
	
	@Bean
	FileManagementService fileManagementService() {
		return mockFactory.Mock(FileManagementService)
	}
	
	@Bean
	TranslateRTCWorkToVSTSWork underTest() {
		return new TranslateRTCWorkToVSTSWork()
	}

}