package com.zions.clm.services.cli.action.integration

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.test.context.ContextConfiguration

import com.mongodb.MongoClient
import com.zions.clm.services.ccm.client.RtcRepositoryClient
import com.zions.clm.services.ccm.workitem.CcmWorkManagementService
import com.zions.clm.services.ccm.workitem.WorkitemAttributeManager
import com.zions.clm.services.ccm.workitem.attachments.AttachmentsManagementService
import com.zions.clm.services.ccm.workitem.metadata.CcmWIMetadataManagementService
import com.zions.clm.services.cli.action.work.AllFilter
import com.zions.clm.services.cli.action.work.TranslateRTCWorkToVSTSWork
import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.clm.services.rtc.project.workitems.ClmWorkItemManagementService
import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cache.MongoDBCacheManagementService
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.link.LinkInfo
import com.zions.common.services.mongo.EmbeddedMongoBuilder
import com.zions.common.services.query.IFilter
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.restart.IQueryHandler
import com.zions.common.services.restart.IRestartManagementService
import com.zions.common.services.restart.RestartManagementService
import com.zions.common.services.test.DataGenerationService
import com.zions.common.services.test.generators.IdSetter
import com.zions.common.services.work.handler.IFieldHandler
import com.zions.mr.services.rest.MrGenericRestClient
import com.zions.qm.services.cli.action.test.QmAllFilter
import com.zions.qm.services.cli.action.test.TranslateRQMToMTM
import com.zions.qm.services.test.ClmTestAttachmentManagementService
import com.zions.qm.services.test.ClmTestItemManagementService
import com.zions.qm.services.test.ClmTestManagementService
import com.zions.qm.services.test.TestMappingManagementService
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.test.TestManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient
import com.zions.vsts.services.work.FileManagementService
import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.work.templates.ProcessTemplateService
import groovy.xml.XmlUtil
import groovyx.net.http.ContentType
import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[IntraModuleLinkIntegrationSpecConfig])
class IntraModuleLinkIntegrationSpec extends Specification {
	
	int index = 1
	
	
	
	@Value('${tfs.project:}')
	String tfsProject
	
	@Autowired
	IRestartManagementService restartManagementService
	
	@Autowired
	TranslateRTCWorkToVSTSWork ccmToAdo
	
	@Autowired
	TranslateRQMToMTM qmtoAdo
	
	@Autowired
	ClmTestItemManagementService clmTestItemManagementService
	
	@Autowired
	ClmTestManagementService clmTestManagementService
	
	@Autowired
	DataGenerationService dataGenerationService
	
	@Autowired
	ICacheManagementService cacheManagementService
	
	@Autowired
	IdSetter idSetter
	@Autowired
	ClmTypeSetter clmTypeSetter

	@Autowired
	TestManagementService testManagementService
	
	@Autowired
	WorkManagementService workManagementService
	
	@Autowired
	MemberManagementService memberManagementService

	@Autowired
	TestMappingManagementService testMappingManagementService
	
	@Autowired
	ClmWorkItemManagementService clmWorkItemManagementService
	
	@Autowired
	CcmWorkManagementService ccmWorkManagementService
	

	def 'CCM to QM item linking'() {
		given: 'Setup of RQM to ADO test elements'
		setupRQMToADO()


		and: 'Setup of CCM to ADO work item elements'
		setupCCMToADO()
		
		and: 'Setup of ccm link data'
		setupLinkData()

		
		when: 'call RQM data,links,execution phase'
		def appArgs = new DefaultApplicationArguments(loadQMArgs())
		//restartManagementService.selectedCheckpoint = 'last'
		cacheManagementService.cacheModule = 'QM'
		qmtoAdo.execute(appArgs)
		
		then: 'Validate rqm data'
		true
		
		when: 'Call CCM workdata,worklinks phase'
		appArgs = new DefaultApplicationArguments(loadCCMArgs())
		cacheManagementService.cacheModule = 'CCM'
		restartManagementService.includePhases = 'workdata,worklinks'
		ccmToAdo.execute(appArgs)
		def results = cacheManagementService.getAllOfType('resultData')
		int bugCount = 0
		results.each { key, result ->
			if (result.associatedBugs) {
				bugCount = bugCount + result.associatedBugs.size()
			}
		}

		then: 'validate ccm data'
		bugCount == 4
		
		cleanup: 'Remove all ADO changes'
		cacheManagementService.cacheModule = 'QM'
		testManagementService.cleanupTestItems('', tfsProject, "${tfsProject}\\test")
		String query = "Select [System.Id], [System.Title] From WorkItems Where [System.AreaPath] = 'IntegrationTests'"
		cacheManagementService.cacheModule = 'CCM'
		workManagementService.clean('', tfsProject, query)
	}
	
	def setupRQMToADO()
	{
//		def tpqdata = dataGenerationService.generate('/testdata/TestPlanQueryData.json')
//		File td = new File('./src/integration-test/resources/testplansquery.xml')
//		def fo = td.newDataOutputStream()
//		fo << tpqdata.data
//		fo.close()
		//plans phase
		clmTestManagementService.getCategories(_, _) >> null
		clmTestManagementService.getCustomAttributes(_, _) >> null
		clmTestManagementService.getExecutionResultViaHref(_,_,_) >> {
			index++
			idSetter.id = "1000${index}"
			def result = dataGenerationService.generate('/testdata/executionresultT.xml')
			return [result]
		}
		int i = 1
		def tpQueryResult = dataGenerationService.generate('/testdata/testplansquery.xml')
		clmTestManagementService.getTestPlansViaQuery(_, _) >> tpQueryResult
		def plans = []
		for (int j = 0; j < 2; j++) {
			idSetter.id = "1000${i}"
			def plan = dataGenerationService.generate('/testdata/testplanT.xml')
			plans.add(plan)
			i++
		}
		
		def children = []
		for (int j = 0; j<4; j++) {
			idSetter.id = "1000${i}"
			def testcase = dataGenerationService.generate('/testdata/testcaseT.xml')
			children.add(testcase)
			i++
		}
		def priorities = dataGenerationService.generate('/testdata/priorities.xml')
		def identity = dataGenerationService.generate('/testdata/identity.xml')
		def states = dataGenerationService.generate('/testdata/states.xml')
		//links phase
		index = i
		clmTestManagementService.getTestItem(_) >> { args ->
			String url = args[0]
			//println url
			if (url.endsWith('testplan:546')) {
				return plans[0]
			} else if (url.endsWith('testplan:545')) {
				return plans[1]
			} else if (url.endsWith('testcase:110174')) {
				return children[0]
			} else if (url.endsWith('testcase:133303')) {
				return children[1]
			} else if (url.endsWith('testcase:133304')) {
				return children[2]
			} else if (url.endsWith('testcase:133305')) {
				return children[3]
			} else if (url.contains('literal.priority')) {
				return priorities
			} else if (url.contains('repository.Contributor')) {
				return identity
			} else if (url.contains('planning.common')) {
				return states
			} else if (url.endsWith('executionresult')) {
				index++
				idSetter.id = "1000${index}"
				return dataGenerationService.generate('/testdata/executionresultT.xml')
			} else if (url.contains('testscript')) {
				return dataGenerationService.generate('/testdata/testscriptT.xml')
			}
		}

	}
	
	def setupCCMToADO()
	{
		def wis
	
		def wiMap = [:]
//		def querytracking = dataGenerationService.generate('/testdata/QueryTracking.json')
//		String xml = querytracking.data
//		File qt = new File('./src/integration-test/resources/testdata/wiquery.xml')
//		def of = qt.newDataOutputStream()
//		of << xml
//		of.close()
		cacheManagementService.cacheModule = 'CCM'
		index = 200000
		wis = dataGenerationService.generate('/testdata/wiquery.xml')
		
		clmWorkItemManagementService.getWorkItemsViaQuery(_) >> { 
			return wis 
		}
		clmWorkItemManagementService.nextPage(_) >> {
			return null
		}
		
		int id = -1
		wis.workItem.each { wi ->
			String type = "${wi.type.name.text()}".replace(' ', '%20')
			if (type == 'Anomaly') {
				type = 'Bug'
			}
			clmTypeSetter.type = type
			idSetter.id = "${id}"
			def wichanges = dataGenerationService.generate('/testdata/wichanges.json')
			String aid = "${wi.id.text()}"
			wiMap[aid] = wichanges
			id--
		}
		//Modified aModified = new Modified()
		ccmWorkManagementService.getWorkitem(_) >> { return new Modified() {} }
		
		ccmWorkManagementService.getAllLinks(_, _, _, _) >> { args ->
			List<LinkInfo> retVal = new ArrayList<LinkInfo>()
		}
		
		ccmWorkManagementService.getWIChanges(_, _, _, _) >> { args ->
			String aid = "${args[0]}"
			return wiMap[aid]
		}
		
		ccmWorkManagementService.getWILinkChanges(_, _, _, _) >> { args ->
			String aid = "${args[0]}"
			Closure closure = args[3]
			def wi = cacheManagementService.getFromCache(aid, ICacheManagementService.WI_DATA)
			String type = "${wi.fields.'System.WorkItemType'}"
			if (type == 'Bug') {
				String cid = "${wi.id}"
				
				
				//Stub result link
				Random r = new Random()
				int ci = r.nextInt(4)
				String rid = "100020000${ci+5}-Result"
				def result = cacheManagementService.getFromCache(rid, 'QM', ICacheManagementService.RESULT_DATA)
				def testedByLink = null
				if (result) {
					String title = "${result.testCaseTitle}"
					
					def resultChanges = [method:'patch', requestContentType: ContentType.JSON, contentType: ContentType.JSON, uri: "/IntegrationTests/_apis/test/Runs/${result.testRun.id}/results/${result.id}", query:['api-version':'5.0'], body: []]
					def data = [id: result.id, testCaseTitle:title, associatedBugs: [[id:cid]]]
					resultChanges.body.add(data)
					def changes = [resultChanges: resultChanges, rid: rid]
					closure('Result', changes)
					
					String tcUrl = "https://dev.azure.com/eto-dev/_apis/wit/workItems/${result.testCase.id}"
					testedByLink = [op: 'add', path: '/relations/-', value: [rel: 'Microsoft.VSTS.Common.TestedBy-Forward', url: tcUrl, attributes:[comment: "Tested by"]]]
				}	
				
				//Stub plan link
				wi = cacheManagementService.getFromCache(aid, ICacheManagementService.WI_DATA)
				def wiData = [method:'PATCH', uri: "/_apis/wit/workitems/${cid}?api-version=5.0-preview.3", headers: ['Content-Type': 'application/json-patch+json'], body: []]
				def rev = [ op: 'test', path: '/rev', value: wi.rev]
				wiData.body.add(rev)
				def plan = cacheManagementService.getFromCache('10002-Test Plan WI', 'QM', ICacheManagementService.WI_DATA)
				String url = "https://dev.azure.com/eto-dev/_apis/wit/workItems/${plan.id}"
				def change = [op: 'add', path: '/relations/-', value: [rel: 'System.LinkTypes.Related', url: url, attributes:[comment: "related_test_plan"]]]
				wiData.body.add(change)
				if (testedByLink) {
					wiData.body.add(testedByLink)
				}
				closure('WorkItem', wiData)

			}
		}
	}
	
	def setupLinkData() {
		
	}
	
	private String[] loadCCMArgs() {
		String[] args = [
			'--ccm.template.dir=./src/integration-test/resources/testdata/core_wit_templates',
			'--clm.projectArea=Integration',
			'--tfs.areapath=IntegrationTests\\work',
			'--tfs.project=IntegrationTests',
			'--wit.mapping.file=./src/integration-test/resources/testdata/CoreCCMMapping.xml',
			'--wi.query=none',
			'--wi.filter=allfilter',
			'--include.update=phases',
			'--meta=meta',
			'--refresh=true'
		]
		return args
	}
	
	private String[] loadQMArgs() {
		String[] args = [
			'--ccm.template.dir=none',
			'--include.update=phases',
			'--include.phases=plans,links,executions',
			'--meta=meta',
			'--tfs.areapath=IntegrationTests\\test',
			'--clm.projectArea=none',
			'--qm.template.dir=none',
			'--test.mapping.file=./src/integration-test/resources/testdata/CoreRQMMapping.xml',
			'--qm.query=none',
			'--qm.filter=qmAllFilter',
			'--tfs.project=IntegrationTests'
		]
		return args
	}

}

@TestConfiguration
@Profile("integration-test")
@ComponentScan(["com.zions.vsts.services", "com.zions.common.services.test", "com.zions.common.services.restart", "com.zions.qm.services.test.handlers", "com.zions.qm.services.cli.action.test.query", "com.zions.clm.services.cli.action.work.query", "com.zions.clm.services.cli.action.integration", "com.zions.common.services.cacheaspect"])
@PropertySource("classpath:integration-test.properties")
//@EnableMongoRepositories(basePackages = "com.zions.common.services.cache.db")
class IntraModuleLinkIntegrationSpecConfig {
	def mockFactory = new DetachedMockFactory()
	
	@Value('${tfs.url:}')
	String tfsUrl
	@Value('${tfs.user:}')
	String tfsUser
	@Value('${tfs.token:}')
	String tfsToken
	
	@Bean
	Map<String, IFilter> filterMap()
	{
		return ['qmAllFilter': new QmAllFilter(), 'allFilter': new AllFilter()]
	}

	// CCM Beans
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
	JavaMailSender sender() {
		return new JavaMailSenderImpl()
	}

	@Bean
	IGenericRestClient genericRestClient() {
		return new GenericRestClient(tfsUrl, tfsUser, tfsToken)
	}
	
	@Bean
	IGenericRestClient mrGenericRestClient() {
		return new MrGenericRestClient(tfsUrl,tfsUser)
	}
	
	@Bean
	IGenericRestClient clmGenericRestClient() {
		//ClmGenericRestClient c
		return mockFactory.Mock(ClmGenericRestClient, name: 'clmGenericRestClient')
	}
	
	@Bean
	IGenericRestClient qmGenericRestClient() {
		return clmGenericRestClient()
	}

	@Bean
	ClmWorkItemManagementService clmWorkItemManagementService() {
		//return new ClmWorkItemManagementService()
		return mockFactory.Stub(ClmWorkItemManagementService)
	}
	
//	@Bean
//	Map<String, IFieldHandler> fieldMap() {
//		return ['filter':iFilter()]
//	}
	
	@Bean
	WorkitemAttributeManager workitemAttributeManager() {
		return mockFactory.Mock(WorkitemAttributeManager)
	}
	
	@Bean
	CcmWorkManagementService ccmWorkManagementService() {
		return mockFactory.Stub(CcmWorkManagementService)
	}
	
	
	@Bean
	AttachmentsManagementService attachmentsManagementService() {
		return mockFactory.Mock(AttachmentsManagementService)
	}
	//End CCM Beans
	
	//RQM Beans
	@Bean
	ClmTestAttachmentManagementService clmTestAttachmentManagementService() {
		return new ClmTestAttachmentManagementService()
	}
	
	@Bean
	ClmTestItemManagementService clmTestItemManagementService() {
		return new ClmTestItemManagementService()
	}
	
	@Bean
	ClmTestManagementService clmTestManagementService() {
		return mockFactory.Stub(ClmTestManagementService)
	}
	
	@Bean
	TestMappingManagementService testMappingManagementService() {
		return new TestMappingManagementService()
	}

	
	@Bean
	TranslateRTCWorkToVSTSWork ccmToAdo() {
		return new TranslateRTCWorkToVSTSWork()
	}
	
	@Bean
	TranslateRQMToMTM qmtoAdo() {
		return new TranslateRQMToMTM()
	}

	@Bean
	DataGenerationService dataGenerationService() {
		return new DataGenerationService()
	}
	
	// VSTS
	@Bean
	MemberManagementService memberManagementService() {
		new MemberManagementService()
	}
	
	@Bean
	CommandManagementService commandManagementService() {
		return new CommandManagementService();
	}

	
//	public MongoClient mongoClient() throws Exception {
//		
//		return new EmbeddedMongoBuilder()
//			.version('3.2.16')
//			.downloadPath('file:./../zions-common-data/mongodb/')
//			.bindIp("127.0.0.1")
//			.port(12345)
//			.build();
//	}
//
//	
//	public @Bean MongoTemplate mongoTemplate() throws Exception {
//		return new MongoTemplate(mongoClient(), getDatabaseName());
//	}
//
//	protected String getDatabaseName() {
//		// TODO Auto-generated method stub
//		return 'coredev';
//	}
	
	@Bean
	IRestartManagementService restartManagementService() {
		return new RestartManagementService()
	}

	
	@Bean
	ICacheManagementService cacheManagementService() {
		return new CacheManagementService()
	}
}

trait Modifiable {
	Date modified() {
		return new Date().previous()
		
	}
}

class Modified implements Modifiable {
	
}