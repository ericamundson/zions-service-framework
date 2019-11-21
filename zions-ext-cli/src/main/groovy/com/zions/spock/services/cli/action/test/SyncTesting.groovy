package com.zions.spock.services.cli.action.test

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.rest.IGenericRestClient
import com.zions.spock.services.test.SpockQueryService
import com.zions.vsts.services.test.TestManagementService
import com.zions.vsts.services.work.ChangeListManager
import com.zions.vsts.services.work.WorkManagementService
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import groovyx.net.http.ContentType
import org.apache.commons.lang.StringEscapeUtils

/**
 * Create spock report with test plan and associated test case.
 * 
 * <p><b>Design:</b></p>
 * <img src="SyncTesting_class_diagram.png"/>
 * @author z091182
 * 
 * @startuml SyncTesting_class_diagram.png
 * annotation Autowired
 * annotation Component
 * class SyncTesting {
 * 
 * .. called by CliApplication to execute test case reporting behavior ..
 * +execute(ApplicationArguments args)
 * 
 * }
 * SyncTesting .. Autowired : annotate members for Spring instance
 * SyncTesting .. Component: define type as Spring object
 * package com.zions.vsts.services.work {
 * SyncTesting --> WorkManagementService: @Autowired workManagmentService
 * }
 * SyncTesting --> com.zions.spock.services.test.SpockQueryService: @Autowired spockQueryService
 * SyncTesting --> com.zions.common.services.cache.ICacheManagementService: @Autowired cacheManagementService
 * SyncTesting --> com.zions.vsts.services.test.TestManagementService: @Autowired testManagementService
 * SyncTesting --> com.zions.common.services.rest.IGenericRestClient: @Autowired genericRestClient
 * @enduml
 * 
 *
 */
@Component
@Slf4j
class SyncTesting implements CliAction {

	@Autowired
	SpockQueryService spockQueryService

	@Autowired
	ICacheManagementService cacheManagementService

	@Value('${ado.area.path:}')
	String areaPath

	@Value('${ado.iteration.path}')
	String iterationPath

	@Value('${tfs.project}')
	String project

	@Value('${tfs.url}')
	String adoUrl

	@Value('${main.tag:}')
	String mainTag

	@Value('${plan.name:}')
	String planName

	@Value('${build.id:}')
	String buildId
	
	@Value('${definition.id:}')
	String definitionId

	def resultMap = ['PASS':'Passed', 'FAIL': 'Failed', 'IGNORED': 'NotExecuted']

	@Autowired
	WorkManagementService workManagementService

	int id = 0

	int newId() {
		id = id - 1
	}
	@Autowired
	TestManagementService testManagementService

	@Autowired
	IGenericRestClient genericRestClient

	/**
	 * 
	 * <p><b>Flow:</b></p>
	 * <img src="execute_sequence_diagram.png"/>
	 * 
	 * @startuml execute_sequence_diagram.png
	 * participant "SyncTesting:this" as this
	 * participant "WorkManagementService:workManagementService" as wms
	 * participant "File:reportSearchPath" as searchPath
	 * participant "SpockQueryService:spockQueryService" as queryService
	 * this -> wms: refreshCacheByQuery('', project, query, keyClosure): void
	 * this -> searchPath: exists: boolean as exists
	 * alt exists == true
	 *    participant "ChangeListManager:clManager" as clm
	 *    this -> clm: <<new>>
	 * 	  this -> queryService: loadAllReports(searchPath) : allTestCase
	 *    loop allTestCase.each testCase
	 *    	 this -> this: buildRequest(testCase) : request
	 *       this -> clm: add(request)
	 *    end
	 *    this -> clm: flush()
	 *    this -> this: buildAndExecute(allTestCase)
	 * end
	 * @enduml
	 *  
	 * @see com.zions.common.services.cli.action.CliAction#execute(org.springframework.boot.ApplicationArguments)
	 */
	@Override
	public Object execute(ApplicationArguments data) {
		String query = "SELECT [System.Id],[System.WorkItemType],[System.Title],[Microsoft.VSTS.Common.Priority],[System.AssignedTo],[System.AreaPath] FROM WorkItems WHERE [System.TeamProject] = '${project}' AND [System.WorkItemType] IN GROUP 'Microsoft.TestCaseCategory' AND [System.AreaPath] UNDER '${areaPath}' AND [System.Tags] CONTAINS '${mainTag}'"
		//testManagementService.cleanupTestItems('', project, areaPath, query)
		workManagementService.refreshCacheByQuery('', project, query) { wi ->
			String title = "${wi.fields.'System.Title'}"
			return title.bytes.encodeBase64()
		}
		String reportSearchPath = data.getOptionValues('report.search.dir')[0]
		File searchDir = new File(reportSearchPath)
		if (searchDir.exists()) {
			ChangeListManager clManager = new ChangeListManager('', project, workManagementService )
			def allTestCase = spockQueryService.loadAllReports(searchDir)
			allTestCase.each { testcase ->
				def r = buildRequest(testcase)
				String key = "${testcase.title}".bytes.encodeBase64()
				clManager.add(key, r)
			}
			clManager.flush()
			if (planName && planName.length() > 0) {
				buildAndExecute(allTestCase)
			}
		}
		return null
	}

	/**
	 * <p><b>Flow:</b></p>
	 * <img src="buildAndExecute_sequence_diagram.png"/>
	 * 
	 * @param allTestCase
	 * @return
	 * 
	 * @startuml buildAndExecute_sequence_diagram.png
	 * participant "SyncTesting:this" as this
	 * participant "TestManagementService:testManagementService" as testManagementService
	 * participant "ICacheManagementService:cacheManagementService" as cacheManagementService
	 * this -> this: ensurePlan()
	 * this -> testManagementService: createRunData('', project, plan, buildId, true): runData
	 * this -> testManagementService: getResultsTestcaseMap("${runData.url}/results"): resultTestCaseMap
	 * participant "Map:resultTestCaseMap" as resultTestCaseMap
	 * testManagementService -> resultTestCaseMap: <<create>>
	 * Activate  resultTestCaseMap
	 * loop allTestCase.each tc
	 *   this -> cacheManagementService: getFromCache(key, ICacheManagementService.WI_DATA) : adoTestCase
	 *   this -> resultTestCaseMap: get("${adoTestCase.id}"): resultData
	 *   alt resultData
	 *     this -> this: sendExecution(resultData, tc)
	 *   end
	 * end
	 * @enduml
	 */
	def buildAndExecute(def allTestCase) {
		def plan = ensurePlan()
		def runData = testManagementService.createRunData('', project, plan, buildId, true)
		def resultTestCaseMap = testManagementService.getResultsTestcaseMap("${runData.url}/results")
		allTestCase.each { tc ->
			String key = "${tc.title}".bytes.encodeBase64()

			String outcome = "${tc.result}"
			log.info "Test case <${tc.title}>, outcome:  ${outcome}"
			if (resultMap[outcome]) {
				def adoTestCase = cacheManagementService.getFromCache(key, ICacheManagementService.WI_DATA)
				//def runData = testManagementService.createRunData('', project, plan, adoTestCase)
				//def resultTestCaseMap = testManagementService.getResultsTestcaseMap("${runData.url}/results")
				def resultData = resultTestCaseMap["${adoTestCase.id}"]
				if (resultData) {
					sendExecution(resultData, tc)
				} else {
					log.warn "Test case:  <${tc.title}> couldn't get result data"
				}
			}
		}
	}

	/**
	 * @param resultData
	 * @param tc
	 * @return
	 */
	def sendExecution(resultData, tc) {
		String key = "${tc.title}".bytes.encodeBase64()
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')
		String runId = "${resultData.testRun.id}"
		String cid = "${resultData.id}"
		def exData = [method: 'patch', contentType: ContentType.JSON, requestContentType: ContentType.JSON, uri: "/${eproject}/_apis/test/Runs/${runId}/results", query:['api-version':'5.0'], body: [:]]
		def ex = [:]
		ex.id = resultData.id
		ex.testCaseTitle = "${tc.title}"
		ex.priority = 1
		String outcome = "${tc.result}"
		ex.outcome = "${resultMap[outcome]}"
		ex.state = 'Completed'
		ex.durationInMs = tc.duration
		if (buildId && buildId.size()>0) {
			ex.build = [ id: buildId ]
		}
		exData.body = new JsonBuilder([ex]).toPrettyString()
		testManagementService.sendResultChanges('', project, exData, key)

	}
	

	def ensurePlan() {
		def plan = testManagementService.getPlan( '', project, planName )
		def suite = null
		if (!plan) {
			plan = createPlan()
			suite = createSuite(plan)
		} 
//		if (!suite) {
//			suite = testManagementService.getSuite(plan, "${planName}-${mainTag} Suite")
//		}
		return plan
	}

	def createPlan() {
		String id = planName.bytes.encodeBase64()
		//def plan = [requestContentType: ContentType.JSON, contentType: ContentType.JSON, uri: "${genericRestClient.tfsUrl}/${project}/_apis/test/plans", query:['api-version':'5.0'],  body: [:]]
		def plan = [:]
		plan['name'] = "${planName}"
		plan['description'] = 'For automated testing'
		plan['state'] = 'Active'
		plan.area = [name:"${areaPath}"]
		plan['iteration'] = "${project}"
		if (definitionId && definitionId.size()>0) {
			plan['buildDefinition'] = [ id: definitionId ]
		}
		if (buildId && buildId.size()>0) {
			plan['build'] = [ id: buildId ]
		}
		//def planData = testManagementService.sendPlanChanges('', project, plan, id)
		def planData = postPlan(plan)
		def oPlanData = testManagementService.getPlan('', project, planData.name)
		return oPlanData
	}

	def postPlan(plan) {
		String body = new JsonBuilder(plan).toPrettyString()
		def result = genericRestClient.post(
				contentType: ContentType.JSON,
				requestContentType: ContentType.JSON,
				uri: "${genericRestClient.tfsUrl}/${project}/_apis/test/plans",
				body: body,
				query: ['api-version': '5.0']
				)
		return result
	}

	def createSuite(parent) {
		String parentId = parent.id
		String cid = "${parent.rootSuite.id}"
		def suite = [method: 'post', requestContentType: ContentType.JSON, contentType: ContentType.JSON, uri: "/${project}/_apis/test/plans/${parentId}/suites/${cid}", query:['api-version':'5.0'], body: null]
		def s = [:]
		s.parent = parent.rootSuite
		s.suiteType = 'dynamicTestSuite'
		String query = "SELECT [System.Id],[System.WorkItemType],[System.Title],[Microsoft.VSTS.Common.Priority],[System.AssignedTo],[System.AreaPath] FROM WorkItems WHERE [System.TeamProject] = '${project}' AND [System.WorkItemType] IN GROUP 'Microsoft.TestCaseCategory' AND [System.AreaPath] UNDER '${areaPath}' AND [System.Tags] CONTAINS '${mainTag}' ORDER BY [System.Title] ASC"
		s.queryString = query
		s.name = "${planName}-${mainTag} Suite"
		s['iteration'] = project
		suite.body = new JsonBuilder(s).toPrettyString()
		String id = "${planName}-${mainTag} Suite".bytes.encodeBase64()
		def suiteData = testManagementService.sendPlanChanges('', project, suite, id)
		return suiteData
	}

	def buildRequest( testCase ) {
		String key = "${testCase.title}".bytes.encodeBase64()
		def etype = URLEncoder.encode('Test Case', 'utf-8').replace('+', '%20')
		def eproject = URLEncoder.encode(project, 'utf-8').replace('+', '%20')

		String url = "${adoUrl}/${eproject}/_apis/wit/workitems/\$${etype}"
		def tc = cacheManagementService.getFromCache(key, ICacheManagementService.WI_DATA)
		def r = [method:'PATCH', uri: "/${eproject}/_apis/wit/workitems/\$${etype}?api-version=5.0&bypassRules=true&suppressNotifications=true", headers: ['Content-Type': 'application/json-patch+json'], body: []]
		if (tc) {
			r = [method:'PATCH', uri: "/_apis/wit/workitems/${tc.id}?api-version=5.0&bypassRules=true&suppressNotifications=true", headers: ['Content-Type': 'application/json-patch+json'], body: []]
			def i = [op: 'test', path: '/rev', value: "${tc.rev}"]
			r.body.add(i)
		} else {
			def i = [op: 'add', path: '/id', value: "${newId()}"]
			r.body.add(i)
		}
		def areaPath = [op: 'add', path: '/fields/System.AreaPath', value: "${areaPath}"]
		r.body.add(areaPath)
		def iPath = [op: 'add', path: '/fields/System.IterationPath', value: "${project}"]
		r.body.add(iPath)
		def state = [op: 'add', path: '/fields/System.State', value: "Ready"]
		r.body.add(state)
		def reason = [op: 'add', path: '/fields/System.Reason', value: "Completed"]
		r.body.add(reason)
		def tag = [op: 'add', path: '/fields/System.Tags', value: "${mainTag}"]
		r.body.add(tag)
		def automate = [op: 'add', path: '/fields/Custom.Automate', value: true]
		r.body.add(automate)
		def automatedState = [op: 'add', path: '/fields/Custom.AutomationState', value: 'Ready']
		r.body.add(automatedState)
		if (!tc) {
			def title = [op: 'add', path: '/fields/System.Title', value: "${testCase.title}"]
			r.body.add(title)
		}
		//if (testCase.result == 'PASS') {
			String title = "${testCase.title}"
			String steps = buildSteps(testCase.blocks)
			def s = [op: 'add', path: '/fields/Microsoft.VSTS.TCM.Steps', value: "${steps}"]
			r.body.add(s)
		//}
		return r
	}

	String buildSteps(def blocks) {
		def writer = new StringWriter()
		MarkupBuilder stepxml = new MarkupBuilder(new IndentPrinter(new PrintWriter(writer), "", false))
		def steps = []
		def stepi = [actionBlocks: [], resultBlocks: []]
		def side = 'action'
		steps.add(stepi)
		blocks.each { block ->
			String k = "${block.kind}"
			if ((k == 'Setup:' || k == 'Given:')  && stepi.actionBlocks.size() > 0) {
				stepi = [actionBlocks: [], resultBlocks: []]
				steps.add(stepi)
				side = 'action'
			} else if (k == 'When:' && stepi.resultBlocks.size() > 0) {
				stepi = [actionBlocks: [], resultBlocks: []]
				steps.add(stepi)
				side = 'action'
			} else if (k == 'Then:') {
				side = 'result'
			}
			if (side == 'action') {
				stepi.actionBlocks.add(block)
			} else if (side == 'result') {
				stepi.resultBlocks.add(block)
			}
		}
		if (steps.size() == 0) return ''
		int sCount = 2
		stepxml.steps(steps: 0, last: steps.size()+1) {

			String htmla = '<DIV>'
			String htmlr = '<DIV>'
					//htmla += "<p><b>${block.kind}</b> <code>${block.text}</code></p>"
					//htmla += codeOut(block.code)
			steps.each { stepo ->
				stepo.actionBlocks.each { block -> 
					htmla += "<DIV><p><b>${block.kind}</b>  <em><code>${block.text}</code></em></p>${codeOut(block.code)}</DIV><p></p>"
				}
				stepo.resultBlocks.each { block -> 
					htmlr += "<DIV><p><b>${block.kind}</b>  <em><code>${block.text}</code></em></p>${codeOut(block.code)}</DIV><p></p>"
				}
				htmla += "</DIV>"
				htmlr += "</DIV>"
				step(id: sCount, type:'ValidateStep') {
					//StringEscapeUtils u
					String htmlStr = escapeHtml(htmla)
					parameterizedString(isformatted: 'true') {
						mkp.yieldUnescaped htmlStr
					}
					htmlStr = escapeHtml(htmlr)
					parameterizedString(isformatted: 'true') {
						mkp.yieldUnescaped htmlStr
					}

				}
				sCount++
				htmla = '<DIV>'
				htmlr = '<DIV>'
			}
		}
		String outVal = writer.toString()
		return outVal
	}
	
	String codeOut(code) {
		String o = ''
		code.each { String l ->
			o += "<p><code>${l}</code></p>"
		}
		return o
	}

	String escapeHtml(String html) {
		String ohtml = html
		ohtml = ohtml.replace('&', '&amp;')
		ohtml = ohtml.replace('<', '&lt;')
		ohtml = ohtml.replace('>', '&gt;')
		return ohtml
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'report.search.dir']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}

}
