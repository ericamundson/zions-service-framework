package com.zions.clm.services.cli.action.integration

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.test.context.ContextConfiguration
import com.ibm.team.workitem.common.model.IWorkItem
import com.mongodb.MongoClient
import com.zions.clm.services.ccm.client.RtcRepositoryClient
import com.zions.clm.services.ccm.workitem.CcmWorkManagementService
import com.zions.clm.services.ccm.workitem.WorkitemAttributeManager
import com.zions.clm.services.ccm.workitem.attachments.AttachmentsManagementService
import com.zions.clm.services.ccm.workitem.metadata.CcmWIMetadataManagementService
import com.zions.clm.services.cli.action.work.AllFilter
import com.zions.clm.services.cli.action.work.TranslateRTCWorkToVSTSWork
import com.zions.clm.services.cli.action.work.query.WorkdataQueryHandler
import com.zions.clm.services.cli.action.work.query.WorklinksQueryHandler
import com.zions.clm.services.rest.ClmBGenericRestClient
import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.clm.services.rtc.project.workitems.ClmWorkItemManagementService
import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cache.MongoDBCacheManagementService
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.db.DatabaseQueryService
import com.zions.common.services.db.IDatabaseQueryService
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
import com.zions.qm.services.cli.action.test.query.ExecutionsQueryHandler
import com.zions.qm.services.cli.action.test.query.LinksQueryHandler
import com.zions.qm.services.cli.action.test.query.PlansQueryHandler
import com.zions.qm.services.test.ClmTestAttachmentManagementService
import com.zions.qm.services.test.ClmTestItemManagementService
import com.zions.qm.services.test.ClmTestManagementService
import com.zions.qm.services.test.TestMappingManagementService
import com.zions.qm.services.test.handlers.BlankHandler
import com.zions.qm.services.test.handlers.CustomAttributesHandler
import com.zions.rm.services.cli.action.requirements.RequirementsQueryHandler
import com.zions.rm.services.cli.action.requirements.TranslateRmBaseArtifactsToADO
import com.zions.rm.services.requirements.ClmRequirementsFileManagementService
import com.zions.rm.services.requirements.ClmRequirementsItemManagementService
import com.zions.rm.services.requirements.ClmRequirementsManagementService
import com.zions.rm.services.requirements.RequirementsMappingManagementService
import com.zions.rm.services.requirements.handlers.AboutHandler
import com.zions.rm.services.requirements.handlers.ActiveReasonHandler
import com.zions.rm.services.requirements.handlers.ArtifactTypeHandler
import com.zions.rm.services.requirements.handlers.ClosedReasonHandler
import com.zions.rm.services.requirements.handlers.CreatedHandler
import com.zions.rm.services.requirements.handlers.CreatorHandler
import com.zions.rm.services.requirements.handlers.DescriptionHandler
import com.zions.rm.services.requirements.handlers.DetailsOfGapHandler
import com.zions.rm.services.requirements.handlers.DocumentTypeHandler
import com.zions.rm.services.requirements.handlers.EntityTypeHandler
import com.zions.rm.services.requirements.handlers.FrTypeHandler
import com.zions.rm.services.requirements.handlers.FsDocumentNumHandler
import com.zions.rm.services.requirements.handlers.GapNoGapHandler
import com.zions.rm.services.requirements.handlers.IdHandler
import com.zions.rm.services.requirements.handlers.MockupTypeHandler
import com.zions.rm.services.requirements.handlers.NameHandler
import com.zions.rm.services.requirements.handlers.NfrTypeHandler
import com.zions.rm.services.requirements.handlers.ReleaseHandler
import com.zions.rm.services.requirements.handlers.RemarksHandler
import com.zions.rm.services.requirements.handlers.RmBaseAttributeHandler
import com.zions.rm.services.requirements.handlers.SectionTypeHandler
import com.zions.rm.services.requirements.handlers.StateHandler
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.test.TestManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient
import com.zions.vsts.services.tfs.rest.MultiUserGenericRestClient
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
	
	@Autowired
	ClmRequirementsManagementService clmRequirementsManagementService
	
	@Autowired
	RequirementsMappingManagementService requirementsMappingManagementService
	
	@Autowired
	ClmRequirementsItemManagementService clmRequirementsItemManagementService
	
	@Autowired
	TranslateRmBaseArtifactsToADO rmBaseToAdo
	
	@Autowired
	RequirementsQueryHandler requirementsQueryHandler
	
	@Autowired
	WorkdataQueryHandler workdataQueryHandler
	
	@Autowired
	WorklinksQueryHandler worklinksQueryHandler
	
	@Autowired
	LinksQueryHandler linksQueryHandler
	
	@Autowired
	PlansQueryHandler plansQueryHandler
	
	@Autowired
	ExecutionsQueryHandler executionsQueryHandler
	
	@Autowired 
	WorkitemAttributeManager workitemAttributeManager
	
	@Autowired
	IDatabaseQueryService databaseQueryService
	
	@Autowired
	Map<String, IFilter> filterMap
	
	def rmUrls = []

	def 'CCM to QM item linking'() {
		given: 'Setup of RQM to ADO test elements with two test plans and 4 test case each'
		setupRQMToADO()


		and: 'Setup of CCM to ADO work item elements with link data with 4 bugs that will link to test results'
		setupCCMToADOForQMLinks()
				
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
		def results = cacheManagementService.getAllOfType(ICacheManagementService.WI_DATA)
		int resultCount = 0
		results.each { key, wi ->
			wi.relations.each { relation ->
				String url = "${relation.url}"
				if (url.startsWith('vstfs')) {
					resultCount++
				}
			}
		}

		then: 'validate correct number of associated bugs to test results'
		resultCount == 4
		
		cleanup: 'Remove all ADO changes'
		cacheManagementService.cacheModule = 'QM'
		testManagementService.cleanupTestItems('', tfsProject, "${tfsProject}\\test")
		String query = "Select [System.Id], [System.Title] From WorkItems Where [System.TeamProject] = 'IntegrationTests'"
		cacheManagementService.cacheModule = 'CCM'
		workManagementService.clean('', tfsProject, query)
	}
	
	def 'CCM to RM item linking'()
	{
		given: 'Setup of RM to ADO requirements artifacts with 8 requirements artifacts'
		setupRMToADO()
		
		and: 'Setup of CCM to ADO work items with links to the 8 requirements artifacts'
		setupCCMToADOForRMLinks()
		
		when: 'Run translation from RRM to ADO requirement artifacts'
		def appArgs = new DefaultApplicationArguments(loadRMArgs())
		//restartManagementService.selectedCheckpoint = 'last'
		cacheManagementService.cacheModule = 'RM'
		restartManagementService.queryHandlers = ['requirementsQueryHandler': requirementsQueryHandler]
		restartManagementService.includePhases = 'requirements'
		rmBaseToAdo.execute(appArgs)
		def wiData = cacheManagementService.getAllOfType(ICacheManagementService.WI_DATA)
		
		
		then: 'validate existence of requirements work items in ADO'
		wiData.size() == 8
		
		when: 'Run translation of CCM to ADO work items'
		appArgs = new DefaultApplicationArguments(loadCCMArgs())
		cacheManagementService.cacheModule = 'CCM'
		restartManagementService.queryHandlers = ['workdataQueryHandler': workdataQueryHandler, 'worklinksQueryHandler': worklinksQueryHandler]
		restartManagementService.includePhases = 'workdata,worklinks'
		ccmToAdo.execute(appArgs)

		then: 'validate existence of work items with links to Requirement type work items in ADO'
		true
		
		cleanup: 'Cleanup all ADO changes for next integration run.'
		cacheManagementService.cacheModule = 'RM'
		String query = "Select [System.Id], [System.Title] From WorkItems Where [Custom.ExternalID] CONTAINS 'DNG-' AND [System.TeamProject] = 'IntegrationTests'"
		workManagementService.clean('',tfsProject, query)
		
		query = "Select [System.Id], [System.Title] From WorkItems Where [System.AreaPath] = 'IntegrationTests' OR [System.AreaPath] = 'IntegrationTests\\work'"
		cacheManagementService.cacheModule = 'CCM'
		workManagementService.clean('', tfsProject, query)

	}
	
	def 'QM to RM item linking'() {
		setup: 'Setup RM work items()'
		setupRMToADO();
		
		and: 'Setup QM with RM linking'
		setupQMToADOForRMLinks()
		
		when: 'Run RM to ADO element translation'
		def appArgs = new DefaultApplicationArguments(loadRMArgs())
		//restartManagementService.selectedCheckpoint = 'last'
		cacheManagementService.cacheModule = 'RM'
		restartManagementService.queryHandlers = ['requirementsQueryHandler': requirementsQueryHandler]
		restartManagementService.includePhases = 'requirements'
		rmBaseToAdo.execute(appArgs)
		def wiData = cacheManagementService.getAllOfType(ICacheManagementService.WI_DATA)
				
		then: 'validate existence of requirements work items in ADO'
		wiData.size() == 8
		
		when: 'Run QM to ADO element translation'
		appArgs = new DefaultApplicationArguments(loadQMArgs())
		//restartManagementService.selectedCheckpoint = 'last'
		restartManagementService.queryHandlers = ['plansQueryHandler': plansQueryHandler, 'executionsQueryHandler': executionsQueryHandler, 'linksQueryHandler': linksQueryHandler]
		restartManagementService.includePhases = 'plans,links,executions'
		cacheManagementService.cacheModule = 'QM'
		qmtoAdo.execute(appArgs)

		then: 'Validate QM to RM links'
		true
		
		cleanup:
		cacheManagementService.cacheModule = 'QM'
		testManagementService.cleanupTestItems('', tfsProject, "${tfsProject}\\test")
		
		cacheManagementService.cacheModule = 'RM'
		String query = "Select [System.Id], [System.Title] From WorkItems Where [Custom.ExternalID] CONTAINS 'DNG-' AND [System.TeamProject] = 'IntegrationTests'"
		workManagementService.clean('',tfsProject, query)

		
	}
	
	
	//setup behavior.
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
	
	def setupCCMToADOForQMLinks()
	{
		def wis
	
		def wiMap = [:]
		cacheManagementService.cacheModule = 'CCM'
		index = 200000
		wis = dataGenerationService.generate('/testdata/wiquery.xml')
		
		clmWorkItemManagementService.getWorkItemsViaQuery(_) >> { 
			return wis 
		}
		clmWorkItemManagementService.nextPage(_) >> {
			return null
		}
		
		
		ccmWorkManagementService.getAllLinks(_, _, _, _) >> { args ->
			List<LinkInfo> links = new ArrayList<LinkInfo>()
			IWorkItem wi = args[2]
			String type = workitemAttributeManager.getStringRepresentation(wi, wi.getProjectArea(), 'workItemType')
			if (type == 'Anomaly') {
				String id = "${wi.getId()}"
				Random r = new Random()
				int ci = r.nextInt(4)
				String rid = "100020000${ci+5}-Result"
			    def info = new LinkInfo(type: 'affects_execution_result', itemIdCurrent: id, itemIdRelated: rid, moduleCurrent: 'CCM', moduleRelated: 'QM')
				links.add(info)

			    info = new LinkInfo(type: 'related_test_plan', itemIdCurrent: id, itemIdRelated: '10002-Test Plan WI', moduleCurrent: 'CCM', moduleRelated: 'QM')
				links.add(info)
			}
			return links
		}
		
	}
	
	def setupRMToADO() {
		
//		clmRequirementsManagementService.queryForArtifacts(_, _, _, _) >> {
//			def result = dataGenerationService.generate('/testdata/RequirementsQuery1.xml')
//			return result
//		}
		databaseQueryService.query(_, _) >> {
			return dataGenerationService.generate('/testdata/rmFirstQueryResult.json')
		}
		databaseQueryService.initialUrl() >> {
			return 'abigselect/1/1'
		}
		databaseQueryService.pageUrl() >> {
			return 'abigselect/1/2'
		}
		databaseQueryService.nextPage() >> {
			return null
		}

	}
	
	def setupCCMToADOForRMLinks() {
		def wis
		ccmWorkManagementService.rtcRepositoryClient.initializeRTC()
		
		def wiMap = [:]
		cacheManagementService.cacheModule = 'CCM'
		index = 200000
		wis = dataGenerationService.generate('/testdata/wiquery.xml')
		
		clmWorkItemManagementService.getWorkItemsViaQuery(_) >> {
			return wis
		}
		clmWorkItemManagementService.nextPage(_) >> {
			return null
		}
		
		
		ccmWorkManagementService.getAllLinks(_, _, _, _) >> { args ->
			List<LinkInfo> links = new ArrayList<LinkInfo>()
			IWorkItem wi = args[2]
			cacheManagementService.cacheModule = 'RM'
			def reqs = cacheManagementService.getAllOfType(ICacheManagementService.WI_DATA)
			cacheManagementService.cacheModule = 'CCM'
			def ids = []
			reqs.each { key, val ->
				ids.add(key)
			}
			Random r = new Random()
			int i  = r.nextInt(ids.size())
			
			String id = "${wi.getId()}"
			
			def rid = ids.get(i)
			//def req = reqs[rid]
			def info = new LinkInfo(type: 'affects_requirement', itemIdCurrent: id, itemIdRelated: rid, moduleCurrent: 'CCM', moduleRelated: 'RM')
			links.add(info)
			return links
		}
		
		
//		ccmWorkManagementService.getWILinkChanges(_, _, _, _) >> { args ->
//			cacheManagementService.cacheModule = 'RM'
//			def reqs = cacheManagementService.getAllOfType(ICacheManagementService.WI_DATA)
//			def ids = []
//			reqs.each { key, val ->
//				ids.add(key)
//			}
//			cacheManagementService.cacheModule = 'CCM'
//			
//			String aid = "${args[0]}"
//			Closure closure = args[3]
//			
//			def wi = cacheManagementService.getFromCache(aid, ICacheManagementService.WI_DATA)
//			String cid = "${wi.id}"
//			def wiData = [method:'PATCH', uri: "/_apis/wit/workitems/${cid}?api-version=5.0-preview.3", headers: ['Content-Type': 'application/json-patch+json'], body: []]
//			def rev = [ op: 'test', path: '/rev', value: wi.rev]
//			wiData.body.add(rev)
//			
//			Random r = new Random()
//			int i  = r.nextInt(ids.size())
//			
//			def rid = ids.get(i)
//			def req = reqs[rid]
//			String url = "https://dev.azure.com/eto-dev/_apis/wit/workItems/${req.id}"
//			def change = [op: 'add', path: '/relations/-', value: [rel: 'System.LinkTypes.Related', url: url, attributes:[comment: "affects_requirement"]]]
//			wiData.body.add(change)
//			closure('WorkItem', wiData)
//		}
	}
		
	def setupQMToADOForRMLinks() {
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
		
		clmTestItemManagementService.getAllLinks(_, _, _) >> { args -> 
			def typeMap = ['testcase': 'Test Case', 'testsuite': 'Test Suite WI', 'testplan': 'Test Plan WI']
			List<LinkInfo> links = new ArrayList<LinkInfo>()
			def testItem = args[2]
			cacheManagementService.cacheModule = 'RM'
			def reqs = cacheManagementService.getAllOfType(ICacheManagementService.WI_DATA)
			cacheManagementService.cacheModule = 'QM'
			def ids = []
			reqs.each { key, val ->
				ids.add(key)
			}
			Random r = new Random()
			int j  = r.nextInt(ids.size())
			
			String iType = "${testItem.name()}"
			String oType = typeMap[iType]
			String id = "${testItem.webId.text()}-${oType}"
			
			def rid = ids.get(j)
			//def req = reqs[rid]
			def info = new LinkInfo(type: 'requirement', itemIdCurrent: id, itemIdRelated: rid, moduleCurrent: 'QM', moduleRelated: 'RM')
			links.add(info)
			return links

		}

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
			'--tfs.areapath=IntegrationTests\\reqs',
			'--clm.projectArea=none',
			'--qm.template.dir=none',
			'--test.mapping.file=./src/integration-test/resources/testdata/CoreRQMMapping.xml',
			'--qm.query=none',
			'--qm.filter=qmAllFilter',
			'--tfs.project=IntegrationTests'
		]
		return args
	}

	private String[] loadRMArgs() {
		/*
		--include.update=phases 
		--include.phases=requirements 
		--mr.url=http://utmvti0190:8026  
		--clm.url=https://clm.cs.zionsbank.com 
		--clm.user=svc-rtcmigration 
		--clm.password=t35T1ng411rTcM!gR@t10n  
		--clm.pageSize=100 
		--tfs.url=https://dev.azure.com 
		--tfs.collection=eto-dev 
		--tfs.user=robert.huet@zionsbancorp.com  
		--clm.projectAreaUri=_klNSEBNGEeSmasotILOx6w 
		--rm.mapping.file=./CoreRRMMapping.xml 
		--tfs.project=FutureCore 
		--process.name=ZionsAgile --include.update=data 
		--oslc.namespaces="&oslc.prefix=dcterms=<http://purl.org/dc/terms/>&oslc.prefix=nav=<http://jazz.net/ns/rm/navigation%23>&oslc.prefix=rdf=<http://www.w3.org/1999/02/22-rdf-syntax-ns%23>&oslc.prefix=rmTypes=<http://www.ibm.com/xmlns/rdm/types/>&oslc.prefix=rm=<http://www.ibm.com/xmlns/rdm/rdf/>" 
		--oslc.select="&oslc.select=dcterms:modified,dcterms:identifier,rmTypes:ArtifactFormat" 
		--oslc.where="&oslc.where=nav:parent=<https://clm.cs.zionsbank.com/rm/folders/_QMhwgVsGEemdeebbT-QcUQ>" 
		--rm.filter=allFilter 
		--tfs.areapath=FutureCore\Requirements\R3 
		--tfs.projectUri={b95a29af-917f-4762-b4bc-c716e7a33b18} 
		--tfs.projectFolder=FutureCore 
		--tfs.isDefaultTeam=true 
		--tfs.teamGuid=dbe48e2d-5113-471a-976d-eb8c1dffa7c5 
		--tfs.collectionId=1bec1897-29a0-44d0-80a8-670c5ae5ef4a
		 */
		String[] args = [
			'--ccm.template.dir=none',
			'--include.update=phases',
			'--include.phases=requirements',
			'--meta=meta',
			'--tfs.areapath=IntegrationTests\\RM',
			'--tfs.projectUri=none',
			'--clm.projectArea=none',
			'--clm.projectAreaUri=none',
			'--tfs.user=z091182',
			'--rm.filter=rmAllFilter',
			'--oslc.namespaces="&oslc.prefix=dcterms=<http://purl.org/dc/terms/>&oslc.prefix=nav=<http://jazz.net/ns/rm/navigation%23>&oslc.prefix=rdf=<http://www.w3.org/1999/02/22-rdf-syntax-ns%23>&oslc.prefix=rmTypes=<http://www.ibm.com/xmlns/rdm/types/>&oslc.prefix=rm=<http://www.ibm.com/xmlns/rdm/rdf/>"',
			'--oslc.select="&oslc.select=dcterms:modified,dcterms:identifier,rmTypes:ArtifactFormat"',
			'--oslc.where="&oslc.where=nav:parent=<https://clm.cs.zionsbank.com/rm/folders/_QMhwgVsGEemdeebbT-QcUQ>"',
			'--rm.mapping.file=./src/integration-test/resources/testdata/CoreRRMMapping.xml',
			'--tfs.project=IntegrationTests',
			'--tfs.teamGuid=none',
			'--tfs.collectionId=none'
		]
		return args
	}
}

@TestConfiguration
@Profile("integration-test")
@ComponentScan(["com.zions.vsts.services", "com.zions.common.services.test", "com.zions.qm.services.test.handlers", "com.zions.rm.services.requirements.handlers", "com.zions.clm.services.ccm.workitem.handler", "com.zions.common.services.restart", "com.zions.qm.services.cli.action.test.query", "com.zions.clm.services.cli.action.work.query", "com.zions.clm.services.cli.action.integration", "com.zions.common.services.cacheaspect"])
@PropertySource("classpath:integration-test.properties")
@EnableMongoRepositories(basePackages = "com.zions.common.services.cache.db")
class IntraModuleLinkIntegrationSpecConfig {
	def mockFactory = new DetachedMockFactory()
	
	@Value('${tfs.url:}')
	String tfsUrl
	@Value('${tfs.user:}')
	String tfsUser
	@Value('${tfs.token:}')
	String tfsToken
	
	@Value('${clm.url:}')
	String clmUrl
	@Value('${clm.user:}')
	String clmUser
	@Value('${clm.password:}')
	String clmPassword


	
	@Bean
	Map<String, IFilter> filterMap()
	{
		return ['qmAllFilter': new QmAllFilter(), 'allFilter': new AllFilter(), 'rmAllFilter': new com.zions.rm.services.cli.action.requirements.AllFilter()]
	}

	// CCM Beans
	@Bean
	RtcRepositoryClient rtcRepositoryClient() {
		return new RtcRepositoryClient(clmUrl, clmUser, clmPassword)
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
		return new MultiUserGenericRestClient()
	}
	
	@Bean
	IGenericRestClient mrGenericRestClient() {
		return new MrGenericRestClient(tfsUrl,tfsUser)
	}
	
	@Bean
	IGenericRestClient clmGenericRestClient() {
		//ClmGenericRestClient c
		return new ClmGenericRestClient(clmUrl, clmUser, clmPassword)
	}
	
	@Bean
	IGenericRestClient qmGenericRestClient() {
		return clmGenericRestClient()
	}

	@Bean
	ClmWorkItemManagementService clmWorkItemManagementService() {
		//return new ClmWorkItemManagementService()
		return mockFactory.Spy(ClmWorkItemManagementService)
	}
	
//	@Bean
//	Map<String, IFieldHandler> fieldMap() {
//		return ['filter':iFilter()]
//	}
	
	@Bean
	WorkitemAttributeManager workitemAttributeManager() {
		return new WorkitemAttributeManager()
	}
	
	@Bean
	CcmWorkManagementService ccmWorkManagementService() {
		return mockFactory.Spy(CcmWorkManagementService)
	}
	
	
	@Bean
	AttachmentsManagementService attachmentsManagementService() {
		return mockFactory.Mock(AttachmentsManagementService)
	}
	
	@Bean
	WorkdataQueryHandler workdataQueryHandler() {
		return new WorkdataQueryHandler()
	}
	
	@Bean
	WorklinksQueryHandler worklinksQueryHandler() {
		return new WorklinksQueryHandler()
	}
	//End CCM Beans
	
	//RQM Beans
	@Bean
	ClmTestAttachmentManagementService clmTestAttachmentManagementService() {
		return new ClmTestAttachmentManagementService()
	}
	
	@Bean
	ClmTestItemManagementService clmTestItemManagementService() {
		return mockFactory.Spy(ClmTestItemManagementService)
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
	PlansQueryHandler plansQueryHandler() {
		return new PlansQueryHandler()
	}
	
	@Bean
	LinksQueryHandler linksQueryHandler() {
		return new LinksQueryHandler()
	}
	
	@Bean
	ExecutionsQueryHandler executionsQueryHandler() {
		return new ExecutionsQueryHandler()
	}
	//End QM
	
	//Start RM beans
	@Bean
	ClmRequirementsManagementService clmRequirementsManagementService()
	{
		return mockFactory.Spy(ClmRequirementsManagementService)
	}
	
	@Bean
	RequirementsMappingManagementService requirementsMappingManagementService()
	{
		return new RequirementsMappingManagementService()
	}
	
	@Bean
	ClmRequirementsItemManagementService clmRequirementsItemManagementService() {
		return new ClmRequirementsItemManagementService()
	}
	
	@Bean
	ClmRequirementsFileManagementService clmRequirementsFileManagementService() {
		return new ClmRequirementsFileManagementService()
	}
	
	@Bean
	IGenericRestClient rmGenericRestClient() {
		return new ClmGenericRestClient(clmUrl, clmUser, clmPassword)
	}
	
	@Bean
	IGenericRestClient rmBGenericRestClient() {
		return mockFactory.Mock(ClmBGenericRestClient)
	}
	
	@Bean
	TranslateRmBaseArtifactsToADO rmBaseToAdo() {
		return new TranslateRmBaseArtifactsToADO()
	}
	
	@Bean
	RequirementsQueryHandler requirementsQueryHandler() {
		return new RequirementsQueryHandler()
	}
	
	@Bean
	IDatabaseQueryService databaseQueryService() {
		return mockFactory.Stub(DatabaseQueryService)
		
	}
	//End RM beans

	
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
	//End  VSTS

	
	
	//Cache managment beans
	public MongoClient mongoClient() throws Exception {
		
		return new EmbeddedMongoBuilder()
			.version('3.2.16')
			//.tempDir('mongodb')
			.installPath('../zions-common-data/mongodb/win32/mongodb-win32-x86_64-3.2.16/bin')
			.bindIp("localhost")
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
	IRestartManagementService restartManagementService() {
		return new RestartManagementService()
	}

	
	@Bean
	ICacheManagementService cacheManagementService() {
		return new MongoDBCacheManagementService()
	}
}



trait Modifiable {
	Date modified() {
		return new Date().previous()
		
	}
}

class Modified implements Modifiable {
	
}