package com.zions.qm.services.cli.action.test

import java.util.Map

import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.qm.services.test.TestScriptStepsExtractor
import com.zions.qm.services.test.StepResultsExtractor
import com.zions.clm.services.ccm.workitem.CcmWorkManagementService
import com.zions.clm.services.ccm.workitem.attachments.AttachmentsManagementService
import com.zions.clm.services.rtc.project.workitems.ClmWorkItemManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.excel.ExcelManagementService
import com.zions.common.services.logging.FlowInterceptor
import com.zions.common.services.query.IFilter
import com.zions.common.services.restart.IRestartManagementService
import com.zions.qm.services.test.ClmTestAttachmentManagementService
import com.zions.qm.services.test.ClmTestItemManagementService
import com.zions.qm.services.test.ClmTestManagementService
import com.zions.qm.services.metadata.QmMetadataManagementService
import com.zions.qm.services.test.TestMappingManagementService
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.notification.NotificationService
import com.zions.vsts.services.work.ChangeListManager
import com.zions.vsts.services.work.FileManagementService
import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.work.templates.ProcessTemplateService
import com.zions.qm.services.test.handlers.StepsHandler
import com.zions.qm.services.test.handlers.StepResultsHandler
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import groovy.xml.XmlUtil

/**
 * Provides command line interaction to synchronize RQM test planning with VSTS.
 * 
 * <p><b>Command-line arguments:</b></p>
 * <ul>
 * 	<li>translateRQMToMTM - The action's Spring bean name.</li>
 * <ul>
 * <p><b>The following's command-line format: --name=value</b></p>
 * <ul>
 *  <li>clm.url - CLM url</li>
 *  <li>clm.user - CLM userid</li>
 *  <li>clm.password - (optional) CLM password. It can be hidden in props file.</li>
 *  <li>ccm.projectArea - RQM project area</li>
 *  <li>tfs.url - ADO url</li>
 *  <li>tfs.user - ADO user</li>
 *  <li>tfs.token - ADO PAT</li>
 *  <li>tfs.project - ADO project</li>
 *  <li>qm.template.dir - RQM meta-data xml</li>
 *  <li>tfs.areapath - ADO area path to set Test planning items.</li>
 *  <li>test.mapping.file - The xml mapping file to enable field data flow.</li>
 *  <li>qm.query - The xpath RQM testplan query.</li>
 *  <li>qm.filter - the name of filter class to used to pair down items that can't be filtered by query.</li>
 *  <li>include.update - Comma delimited list of the phases that will run during execution. E.G. refresh,clean,data,execution,links,attachments</li>
 *  </ul>
 * </ul>
 * 
 * <p><b>Design:</b></p>
 * <img src="TranslateRQMToMTM_class_diagram.png"/>
 * <p><b>Flow:</b></p>
 * <img src="TranslateRQMToMTM_sequence_diagram.png"/>
 * @author z091182
 * 
 * @startuml TranslateRQMToMTM_class_diagram.png
 * 
 * annotation Autowired
 * annotation Component
 * class Map<? extends String, ? extends IFilter> {
 * 	+ put(key, element)
 * 	+ get(key)
 * .. groovy access/set elements ...
 * 	+ [key] 
 * }
 * class TranslateRQMToMTM {
 * ... Called by CliApplication to validate args ...
 * 	+validate(ApplicationArguments args)
 * 
 * ... Called by CliApplication to execute behavior with args ...
 * 	+execute(ApplicationArguments args)
 * 
 * ...  Filter query result ...
 * 	+filtered(def, String)
 * }
 * 
 * 
 * CliAction <|.. TranslateRQMToMTM
 * TranslateRQMToMTM .. Autowired:  Spring Boot
 * TranslateRQMToMTM .. Component: Spring injectable type
 * TranslateRQMToMTM --> Map: @Autowired filterMap
 * package com.zions.vsts.services.work {
 *  TranslateRQMToMTM --> WorkManagementService: @Autowired workManagementService
 *  TranslateRQMToMTM --> FileManagementService: @Autowired fileManagementService
 *  TranslateRQMToMTM --> ChangeListManager: clManager
 * }
 * TranslateRQMToMTM --> com.zions.qm.services.metadata.QmMetadataManagementService: @Autowired qmMetadataManagementService
 * package com.zions.qm.services.test {
 *  TranslateRQMToMTM --> ClmTestManagementService: @Autowired clmTestManagementService
 *  TranslateRQMToMTM --> ClmTestItemManagementService: @Autowired clmTestItemManagementService
 *  TranslateRQMToMTM --> TestMappingManagementService: @Autowired testMappingManagementService
 * }
 * TranslateRQMToMTM -->  com.zions.vsts.services.admin.member.MemberManagementService: @Autowired memberManagementService
 * TranslateRQMToMTM --> com.zions.vsts.services.test.TestManagementService: @Autowired TestManagementService
 * TranslateRQMToMTM --> com.zions.clm.services.attachments.ClmAttachmentsManagementService: @Autowired clmAttachmentsManagementService
 * TranslateRQMToMTM --> com.zions.common.services.restart.IRestartManagementService: @Autowired restartManagementService
 * @enduml
 * 
 * @startuml TranslateRQMToMTM_sequence_diagram.png
 * participant CliApplication
 * CliApplication -> TranslateRQMToMTM: validate(ApplicationArguments args)
 * CliApplication -> TranslateRQMToMTM: execute(ApplicationArguments args)
 * alt include.update has 'clean'
 * 	TranslateRQMToMTM -> TestManagementService: cleanupTestItems(collection, tfsProject, areaPath)
 * end
 *  TranslateRQMToMTM -> TestMappingManagementService: get field mapping
 *  TranslateRQMToMTM -> MemberManagementService: get member map
 * TranslateRQMToMTM -> "IRestartManagementService:restartManagementService" as restartManagementService: processPhases
 * group restartManagmentService.processPhases closure phase, items ->
 *  alt phase == 'configuration'
 *  	loop each { configuration }
 *  		TranslateRQMToMTM -> ClmTestManagementService: get configuration details
 *  		TranslateRQMToMTM -> ClmTestItemManagementService: get data changes
 *  		TranslateRQMToMTM -> TestManagementService: send configuration changes
 *  	end
 *  end
 *  alt phase == 'testcase'
 *  	loop each test case
 *  		TranslateRQMToMTM -> ClmTestItemManagementService: get work item data changes
 *  		TranslateRQMToMTM -> "ChangeListManager:clManager": add 'Test Case' work item changes
 *  	end
 *  TranslateRQMToMTM -> "ChangeListManager:clManager": flush.
 *  end
 *  alt phase == 'plan'
 *  loop each { testplan object structure }
 *  	TranslateRQMToMTM -> ClmTestItemManagementService: get data changes
 *  	TranslateRQMToMTM -> TestManagementService: sent plan and suite changes
 *  	loop each test suite of test plan
 *  		TranslateRQMToMTM -> ClmTestItemManagementService: get data changes
 *  		TranslateRQMToMTM -> TestManagementService: sent plan and suite changes
 *  	end
 *  end
 *  end
 *  alt phase == 'links'
 *  loop each { testplan object structure }
 *  	loop each testsuite for testplan
 *  		loop each test case for test suite
 *  			TranslateRQMToMTM -> List: add test case to list
 *  		end
 *  		TranslateRQMToMTM -> TestManagementService: setParent of list of test case to test suite
 *  	end
 *  	loop each test case for test plan
 *  		TranslateRQMToMTM -> List: add test case to list
 *  	end
 *  	TranslateRQMToMTM -> TestManagementService: setParent of list of test case to test plan
 *  end
 *  end
 *  alt phase == 'execution'
 *  loop each { testplan object structure }
 *  	TranslateRQMToMTM -> TestManagmentService: ensure test runs for test plan
 *  	loop each { test suite }
 *  		loop each { test case of test suite}
 *  			TranslateRQMToMTM -> CLMTestManagementService: get execution results for test case of this suite
 *  			loop each execution result
 *  				TranslateRQMToMTM -> ClmTestItemManagementService: ensure test result data for testcase
 *  				TranslateRQMToMTM -> TestManagmentService: send test result data
 *  			end
 *  		end
 *  	end
 *  	loop each { test case of test plan}
 *  		TranslateRQMToMTM -> CLMTestManagementService: get execution results for test case of this plan
 *  		loop each execution result
 TranslateRQMToMTM -> ClmTestItemManagementService: ensure test result data for testcase
 *  			TranslateRQMToMTM -> TestManagmentService: send test result data
 *  		end
 *  	end
 *  end
 *  end
 *  alt phase == 'attachments'
 *  	TranslateRQMToMTM -> MemberManagementService: get member map
 *  	TranslateRQMToMTM -> ClmTestManagementService: get test plans via query
 *  	loop each { test plans }
 *  		loop each test suite
 *  			loop each test case
 *  				TranslateRQMToMTM -> ClmTestItemManagementService: get attachment changes for test case
 *  				
 *  				TranslateRQMToMTM -> "ChangeListManager:clManager": add attachment changes 		
 *  			end
 *  		end
 *  	end
 *  	TranslateRQMToMTM -> "ChangeListManager:clManager":flush.
 *  end
 *  end
 * @enduml
 *
 */
@Component
@Slf4j
class ArchiveRQM implements CliAction {
	//	@Autowired
	//	private Map<String, IFilter> filterMap;
	//	@Autowired
	//	QmMetadataManagementService qmMetadataManagementService;
	@Autowired
	TestMappingManagementService testMappingManagementService;
	@Autowired
	WorkManagementService workManagementService;
	@Autowired
	ClmTestManagementService clmTestManagementService;
	@Autowired
	ClmTestItemManagementService clmTestItemManagementService;
	@Autowired
	StepsHandler stepsHandler;
	@Autowired
	StepResultsHandler stepResultsHandler;
	@Autowired
	MemberManagementService memberManagementService;
	//	@Autowired
	//	AttachmentsManagementService attachmentsManagementService
	@Autowired
	FileManagementService fileManagementService;
	@Autowired
	ClmTestAttachmentManagementService clmAttachmentManagementService

	@Autowired
	IRestartManagementService restartManagementService

	@Value('${process.full.plan:false}')
	String processFullPlan

	@Value('${refresh.run:false}')
	boolean refreshRun

	@Value('${update.links:false}')
	boolean updateLinks

	@Value('${parent.plan.name:}')
	String parentPlanName

	@Value('${rm.area.path:}')
	String rmAreaPath

	@Value('${rm.module:RM}')
	String rmModule

	@Value('${plan.id:}')
	String planId

	@Value('${plan.checkpoint:false}')
	boolean planCheckpoint

	@Value('${execution.checkpoint:false}')
	boolean executionCheckpoint

	@Value('${link.checkpoint:false}')
	boolean linkCheckpoint

	@Value('${update.cache.only:false}')
	boolean updateCacheOnly


	@Autowired
	ICacheManagementService cacheManagementService
	
	@Autowired
	ExcelManagementService excelManagementService
	
	@Autowired
	@Value('${excel.path}')
	String filePath
	
	def priorities = null
	def cachedEmails = [:]
	
	public ArchiveRQM() {
	}

	/* (non-Javadoc)
	 * @see com.zions.common.services.cli.action.CliAction#execute(org.springframework.boot.ApplicationArguments)
	 */
	public def execute(ApplicationArguments data) {
		boolean excludeMetaUpdate = true
		def includes = [:]
		try {
			String includeList = data.getOptionValues('include.update')[0]
			def includeItems = includeList.split(',')
			includeItems.each { item ->
				includes[item] = item
			}
		} catch (e) {}
		String areaPath = data.getOptionValues('tfs.areapath')[0]
		String project = data.getOptionValues('clm.projectArea')[0]
		String templateDir = data.getOptionValues('qm.template.dir')[0]
		String mappingFile = data.getOptionValues('test.mapping.file')[0]
		String qmQuery = data.getOptionValues('qm.query')[0]
		String wiFilter = data.getOptionValues('qm.filter')[0]
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		String tfsProject = data.getOptionValues('tfs.project')[0]
		def suiteMap = null
		String phases = ""
		if (includes['phases'] != null) {
			restartManagementService.processPhases { phase, items ->
				if (phase == 'plans') {
					def planTestCaseIds = []
					if (!planCheckpoint) {
						cacheManagementService.deleteByType('psuiteCheckpoint')
					}
					phases += 'plans'
					int suiteCount = 1
//					ChangeListManager clManager = new ChangeListManager(collection, tfsProject, workManagementService )
					def idKeyMap = [:]
					clmTestItemManagementService.resetNewId()
					def itemCount = 0
					items.each { testItem ->
						itemCount++
						def planDir = getTestPlanDir(testItem)
						// Create a directory for this test plan.  If it already exists, skip it
						File targetDir = new File(planDir)
						if (!targetDir.exists()) {
							targetDir.mkdir()
						}
						else {
							log.info("Skipping $planDir.  Already archived.")
							return // skip this plan.  Should be already archived
						}
						def testplan = clmTestManagementService.getTestItem(testItem.id.text())
						int planId = Integer.parseInt(testplan.webId.text())
						log.info ">>>>> Archiving plan #${itemCount} of ${items.size()}: ${testplan.title}"
						
						def attachmentDir = "$planDir\\Attachments"
						File targetAttachmentDir = new File(attachmentDir)
						try {
							targetAttachmentDir.mkdir()
						}
						catch (Exception e){
							log.info("Attachments Directory for plan '${testplan.title}' already exists")
						}

						// Create directory Excel for Test Plan
						excelManagementService.CreateExcelFile(planDir,"Test Plan")
						def tpAttributes = getTestPlanAttributes(testplan)
						insertArtifactIntoExcel("$planId", 'Test Plan', tpAttributes)
						
						// Populate Test Suites
						log.info "Archiving Suites for: ${testplan.title}"
						excelManagementService.CreateExcelFile(planDir,"Test Suites")
						def testSuites = testplan.'**'.findAll { p ->
							"${p.name()}" == 'testsuite'
						}
						testSuites.each { suite ->
							String suiteHref = "${suite.@href}"
							String sId = this.getIdFromHref(suiteHref)
							def tsAttributes = getTestSuiteAttributes(suiteHref)
							insertArtifactIntoExcel("$sId", 'Test Suite', tsAttributes)
						}

						// Populate Test Cases
						log.info "Archiving Test Cases for: ${testplan.title}"
						excelManagementService.CreateExcelFile(planDir,"Test Cases")
						

						Long iCount = 0
						testplan.testcase.each { tc ->
							if (++iCount%100 == 0) println("Count: $iCount")
							outputTestCase(tc, '', planTestCaseIds, attachmentDir)
						}
						
						// Populate Test Cases that are in Suites
						String planIdentifier = "${testplan.identifier.text()}"
						testplan.testsuite.each { testsuiteRef ->
							def testsuite = clmTestManagementService.getTestItem("${testsuiteRef.@href}")
							if (testsuite.suiteelements) {
								String suitewebId = "${testsuite.webId.text()}"
								def atcs = []
								def ids = [:]
								def allIds = [:]

								def exData = null
								Set<String> points = []
								String suiteKey = "${suitewebId}-Inner Test Suite"
								testsuite.suiteelements.suiteelement.each { suiteelement ->
									if (suiteelement.testcase) {
										outputTestCase(suiteelement.testcase, suitewebId, planTestCaseIds, attachmentDir)
									}
								}
							}
						}

						// Populate Test Results from test suites
						log.info "Archiving Test Results: ${testplan.title}"
						excelManagementService.CreateExcelFile(planDir,"Test Results")
						
						// Populate Test Results
						planTestCaseIds.toUnique().each { tcwebId ->
							def executionresults = clmTestManagementService.getExecutionResultViaHref(tcwebId, "$planId", project)
							if (executionresults.size() > 0) {
								executionresults.each { result ->
									outputTestResult(result, attachmentDir)
								}
							}
						}
					}	
					// Close out 
					excelManagementService.CloseExcelFile()

				}
			}
		}
		//notificationService.sendActionCompleteNotification('translateRQMToAdoForCore', phases)
		//extract & apply attachments.

		//ccmWorkManagementService.rtcRepositoryClient.shutdownPlatform()
	}
	def getTestPlanAttributes( def testplan ) {
		def dir
		return getTestItemAttributes(testplan, dir)
	}
	def getTestSuiteAttributes( def suiteHref ) {
		def dir
		def attrMap = [:]
		def testsuite = clmTestManagementService.getTestItem(suiteHref)
		attrMap = getTestItemAttributes(testsuite, dir)
		
		// Now add an attribute to hold all test cases for the suite
		String tcString = ''
		def suiteElements = testsuite.'**'.findAll { p ->
			"${p.name()}" == 'suiteelement' }
		suiteElements.each {  node ->
			def tcHref = "${node.testcase.@href}"
			def tcId = this.getIdFromHref(tcHref)
			if (tcString != '') tcString = tcString + '\n '
			tcString = tcString + tcId
		}
		def key = 'Test Cases'
		attrMap.putAt(key, tcString)
		return attrMap
	}
	def getTestCaseAttributes( def testcase, def testscript, def suiteId, def stepsExtractor, def dir ) {
		def attrMap = getTestItemAttributes(testcase, dir)
		attrMap.put("Suite ID", suiteId)
		
		// Now add script attributes
		if (testscript != null) {
			def scriptAttributes = getTestItemAttributes(testscript, dir,'SCRIPT-')
			attrMap = attrMap + scriptAttributes
		}
		
		// Now add first step attributes
		if (stepsExtractor != null && stepsExtractor.steps.size() > 0 ) {
			def prefix = 'STEP-'
			attrMap.put(prefix + '#',"1")
			attrMap.put(prefix + 'Description',"${stepsExtractor.steps[0].description}")
			attrMap.put(prefix + 'Expected Result',"${stepsExtractor.steps[0].expectedResult}")
			attrMap.put(prefix + 'Attachment',"${stepsExtractor.steps[0].attachment}")
		}
		
		return attrMap
	}
	def getResultAttributes( def result, def stepResultsExtractor, def dir ) {
		def attrMap = getTestItemAttributes(result, dir)
		
		// Now add first step attributes
		if (stepResultsExtractor != null && stepResultsExtractor.steps.size() > 0 ) {
			def prefix = 'STEP-'
			attrMap.put(prefix + '#',"1")
			attrMap.put(prefix + 'Description',"${stepResultsExtractor.steps[0].description}")
			attrMap.put(prefix + 'Expected Result',"${stepResultsExtractor.steps[0].expectedResult}")
			attrMap.put(prefix + 'Result',"${stepResultsExtractor.steps[0].result}")
			attrMap.put(prefix + 'End Time',"${stepResultsExtractor.steps[0].endTime}")
			attrMap.put(prefix + 'Attachment',"${stepResultsExtractor.steps[0].attachment}")
		}
		
		return attrMap
	}

	def getTestItemAttributes( def item, def dir, String prefix='') {
		def attrMap = [:]
		item.children().each {  node ->
			def key = "${node.name()}"
			def val
			
			if ( "${node.name()}" == 'scriptStepCount' ||
				"${node.name()}" == 'BODY' ||
				"${node.name()}" == 'HEAD' ||
				"${node.name()}" == 'testscript' ||
				"${node.name()}" == 'testcasestateexecution' ||
				"${node.name()}" == 'template' ||
				"${node.name()}" == 'stateid' ||
				"${node.name()}" == 'authoid' ||
				"${node.name()}" == 'ownerid' ||
				"${node.name()}" == 'suiteelements' ||
				"${node.name()}" == 'alias' ||
				"${node.name()}" == 'approvals' ||
				"${node.name()}" == 'com.ibm.rqm.planning.editor.section.planNormativeInformative' ||
				"${node.name()}" == 'identifier' ||
				"${node.name()}" == 'weight' ||
				"${node.name()}      ".substring(0,6) == 'points' ||
				"${node.name()}" == 'variables' ||
				"${node.name()}" == 'artifactAttributes' ||
				"${node.name()}" == 'stylesheet' ) {
				return
			}
			else if ( "${item.name()}" == 'testplan' && 
				("${node.name()}" == 'testcase' || "${node.name()}" == 'testsuite')) {
				return
			}
			else if ( "${node.name()}" == 'category') {
				// Get attributes from child node
				key = "${node.@term}"
				val = "${node.@value}"
			}
			else if ( "${node.name()}" == 'projectArea') {
				if (prefix == '') {
					val = "${node.@alias}".replace('+',' ').replace('%28','(').replace('%29', ')')
				}
				else {
					return // Only need projectArea once in the archive
				}
			}
			else {
				// Get first child value from child node
				def childNode = node.childNodes().this$0
				val = "${childNode.children()[0]}"
				if ( key == 'creator' || key == 'owner' || key == 'approvalOwner' || key == 'approval' || key == 'testedby' || key == 'authorid') {
					def ref 
					if ( key == 'testedby') {
						ref = childNode.children()[0].attributes()
					}
					else {
						ref = childNode.attributes()
					}
					// Get user email
					def userUrl = "${ref.'{http://www.w3.org/1999/02/22-rdf-syntax-ns#}resource'}"
					def cachedVal = cachedEmails[userUrl]
					if (cachedVal == null) {
						def userInfo = clmTestManagementService.getTestItem(userUrl)
						if (userInfo != null) {
							val = "${userInfo.emailAddress.text()}".toLowerCase()
							cachedEmails.put(userUrl,val)
						}
						else if (val == 'unassigned') {
							cachedEmails.put(userUrl, 'unassigned')
						}
					}
					else  {
						val = cachedVal
					}
				}
				else if ( key == 'priority') {
					def url = "${node.'@ns7:resource'}"
					// Get literal string
					def ps = getPriorities(url)
					val = ps.find { p ->
						String id = "${p.identifier.text()}"
						id == val
					}
					if (val != null) {
						val = val.title.text()
					}
					else {
						val = ''
					}
				}
				else if ( "${item.name()}" == 'executionresult' && "${node.name()}" == 'state') {
					val = val.replace('com.ibm.rqm.execution.common.state.','')
				}
				else if ( "${item.name()}" == 'scripttype') {
					val = val.replace('com.ibm.rqm.planning.common.scripttype.','')
				}
				else if ( key == 'state') {
					def ref = childNode.attributes()
					// Get literal string
					def stateUrl = "${ref.'{http://www.w3.org/1999/02/22-rdf-syntax-ns#}resource'}"
					def stateInfo = clmTestManagementService.getTestItem(stateUrl)
					if (stateInfo) {
						def stateItem = stateInfo.ProcessInfo.hasWorkflowState.WorkflowState.find { state ->
							"${state.@'rdf:about'}" == "${stateUrl}"
						}
						if (stateItem != null) {
							val = "${stateItem.title.text()}"
						}
					}
				}
				else if ( key == 'testcase' || key == 'testplan' || key == 'executionworkitem') {
					def tcHref = "${node.@href}"
					val = this.getIdFromHref(tcHref)
				}
				else if ( key == 'defect') {
					val = "${node.@rel} ${node.@summary}"
				}
				else if ( key == 'steps') {
					key = 'stepCount'
					val = "${node.children().size()}"
				}
				else if ( key == 'stepResults') {
					val = ''
					return
				}
				else {
					val = "${node.toString()}"
					if ( (key == 'com.ibm.rqm.planning.editor.section.testCasePreCondition' ||
						  key == 'com.ibm.rqm.planning.editor.section.testCasePostCondition') && val != '') {
					  	val = ''
						node.depthFirst().findAll({ "${it.name()}" == 'p' || "${it.name()}" == 'div'}).each { p ->
							def child = p[0]
							def children = child.children()
							val = val + getLinkedListContent(children)
						}
					}
				}
			}
			if (val != '') attrMap.put(prefix + key, val)
		}
		
		return attrMap
	}
	
	def getLinkedListContent( def list ) {
		def val = ''
		list.each {
			if ("${it.class.name}" == 'java.lang.String') {
				val = "$val$it\n"
			}
		}
		return val
	}


	def insertArtifactIntoExcel(String id, String type, def attributeMap) {
		//each attribute must be inserted into the workbook without making it too messy
		//perhaps ExcelManager can add columns to current row based on insertion by header
		//log.debug("wi made it to insert method")
		//log.debug("'ID': ${wi.ID}, 'wi Type': ${wi.wiType}")
		
		excelManagementService.InsertNewRow(['webId': "$id", 'Type': "$type"])
		attributeMap.each { key, value ->
			try {
			excelManagementService.InsertIntoCurrentRow(value, key)
			} catch (Exception e){
				log.error("Artifact $id had error writing to spreadsheet: ${e}")
			}
		}
		
	}


	/* (non-Javadoc)
	 * @see com.zions.common.services.cli.action.CliAction#validate(org.springframework.boot.ApplicationArguments)
	 */
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['clm.url', 'clm.user', 'clm.projectArea', 'qm.template.dir', 'tfs.url', 'tfs.user', 'tfs.project', 'tfs.areapath', 'test.mapping.file', 'qm.query', 'qm.filter']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}


	private def getIdFromHref(String href) {
		 return href.substring(href.lastIndexOf(':')+1, href.length())
	}
	
	def archiveFile(String fname, String dir, byte[] byteArray) {
		// Write out file
		try {
			new File("$dir/$fname").withOutputStream {
				it.write byteArray
			}
		}
		catch (e) {
			log.error("Could not save file $fname.  Error: ${e.getMessage()}")
		}
	}
	def getPriorities(String url) {
		if (priorities != null && priorities.size() > 0) return priorities
		def priorityStuff = clmTestManagementService.getTestItem(url)
		priorities = priorityStuff.'**'.findAll { node ->
			node.name() == 'Priority'
		}
		return priorities
	}

	def outputTestCase(def tc, def suitewebId, def planTestCaseIds, def attachmentDir) {
		String tcHref = "${tc.@href}"
		def testcase = clmTestManagementService.getTestItem(tcHref)
		String tcId = "${testcase.webId}"
		if (tcId == null || tcId == '') {
			log.error("ERROR: Session timed out!!! Failed to retrieve Test Case: $tcHref")
			System.exit(1)
		}
		planTestCaseIds.add(tcId)
		def testscriptNode = testcase.'**'.find {"${it.name()}" == 'testscript' }
		def testscript
		def stepsExtractor
		if (testscriptNode) {
			testscript = clmTestManagementService.getTestItem("${testscriptNode.@href}")
			stepsExtractor = new TestScriptStepsExtractor(testscript, stepsHandler, clmAttachmentManagementService, attachmentDir)
		}
		def tsAttributes = getTestCaseAttributes(testcase, testscript, "${suitewebId}", stepsExtractor, attachmentDir)
//							String tcId = tsAttributes.webId
		insertArtifactIntoExcel(tcId, 'Test Case', tsAttributes)
		// Now insert additional steps for the Test Script
		if (stepsExtractor != null) {
			def prefix = "STEP-"
			def count = 0
			stepsExtractor.steps.each { step->
				count++
				if (count == 1) return // only outputting steps 2 +
				def stepMap = [:]
				stepMap.put(prefix + '#',"$count")
				stepMap.put(prefix + 'Description',"$step.description")
				stepMap.put(prefix + 'Expected Result',"$step.expectedResult")
				stepMap.put(prefix + 'Attachment',"$step.attachment")
				insertArtifactIntoExcel(tcId, 'Test Case', stepMap)
			}
		}

	}
	def outputTestResult(def result, def attachmentDir) {
		def stepResultsExtractor = new StepResultsExtractor(result, stepResultsHandler, clmAttachmentManagementService, attachmentDir)
		def resultAttrList = getResultAttributes(result, stepResultsExtractor, attachmentDir)
		insertArtifactIntoExcel('', 'Test Result', resultAttrList)
		// Now insert additional steps for the Test Script
		if (stepResultsExtractor != null) {
			def prefix = "STEP-"
			def count = 0
			stepResultsExtractor.steps.each { step->
				count++
				if (count == 1) return // only outputting steps 2 +
				def stepMap = [:]
				stepMap.put(prefix + '#',"$count")
				stepMap.put(prefix + 'Description',"$step.description")
				stepMap.put(prefix + 'Expected Result',"$step.expectedResult")
				stepMap.put(prefix + 'Result',"$step.result")
				stepMap.put(prefix + 'End Time',"$step.endTime")
				stepMap.put(prefix + 'Attachment',"$step.attachment")
				insertArtifactIntoExcel('', 'Test Result', stepMap)
			}
		}
	}
	String getTestPlanDir(def testItem) {
		return "$filePath\\${testItem.content.testplan.webId.text()} - " + "${testItem.title}".replace('/','-').replace(':',' ')
	}
}

