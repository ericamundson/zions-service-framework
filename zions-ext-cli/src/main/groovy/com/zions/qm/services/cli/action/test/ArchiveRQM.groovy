package com.zions.qm.services.cli.action.test

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

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
import com.zions.vsts.services.test.TestManagementService
import com.zions.vsts.services.work.ChangeListManager
import com.zions.vsts.services.work.FileManagementService
import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.work.templates.ProcessTemplateService
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
	MemberManagementService memberManagementService;
	//	@Autowired
	//	AttachmentsManagementService attachmentsManagementService
	@Autowired
	FileManagementService fileManagementService;
	@Autowired
	TestManagementService testManagementService;
	@Autowired
	ClmTestAttachmentManagementService clmAttachmentManagementService

	@Autowired
	IRestartManagementService restartManagementService

	@Autowired
	NotificationService notificationService

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
		File mFile = new File(mappingFile)

		def mapping = new XmlSlurper().parseText(mFile.text)
		def testTypes = loadTestTypes(templateDir)
		def mappingData = testMappingManagementService.mappingData
		def memberMap = memberManagementService.getProjectMembersMap(collection, tfsProject)
		def parentPlan = testManagementService.getPlan(collection, tfsProject, parentPlanName)
		def suiteMap = null
		String phases = ""
		if (includes['phases'] != null) {
			restartManagementService.processPhases { phase, items ->
				if (phase == 'plans') {
					if (!planCheckpoint) {
						cacheManagementService.deleteByType('psuiteCheckpoint')
					}
					phases += 'plans'
					int suiteCount = 1
					ChangeListManager clManager = new ChangeListManager(collection, tfsProject, workManagementService )
					def idKeyMap = [:]
					clmTestItemManagementService.resetNewId()
					items.each { testItem ->
						
						def testplan = clmTestManagementService.getTestItem(testItem.id.text())
						int planId = Integer.parseInt(testplan.webId.text())
						log.info "Archiving Plan: ${testplan.title}"
						
						// Create a directory for this test plan
						def planDir = "$filePath/${testplan.webId.text()} - ${testplan.title}"
						File targetDir = new File(planDir)
						try {
							targetDir.mkdir()
						}
						catch (Exception e){
							log.info("Directory for plan '${testplan.title}' already exists")
						}
						def attachmentDir = "@planDir/Attachments"
						File targetAttachmentDir = new File(planDir)
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
						def testCases = testplan.'**'.findAll { p ->
							"${p.name()}" == 'testcase'
						}
						
						testCases.each { tc ->
							String tcHref = "${tc.@href}"
							String tcId = this.getIdFromHref(tcHref)
							def testcase = clmTestManagementService.getTestItem(tcHref)
							def testscriptNode = testcase.'**'.find {"${it.name()}" == 'testscript' }
							def testscript = clmTestManagementService.getTestItem("${testscriptNode.@href}")
							def prefix = 'script-'
							def tsAttributes = getTestCaseAttributes(testcase, testscript, prefix)
							insertArtifactIntoExcel("$tcId", 'Test Case', tsAttributes)
							def additionalSteps = getAdditionalSteps(testscript)
							additionalSteps.each { step->
								def stepMap = [:]
								stepMap.put(prefix + 'steps',"$step")
								insertArtifactIntoExcel("$tcId", 'Test Case', stepMap)
							}
							
					
							
						}

						// Populate Test Results
						log.info "Archiving Test Results: ${testplan.title}"
						excelManagementService.CreateExcelFile(planDir,"Test Results - ${testplan.title}")
						testCases.each { tc ->
							String tcHref = "${tc.@href}"
							def executionresults = clmTestManagementService.getExecutionResultViaHref(this.getIdFromHref(tcHref), "$planId", project)
							if (executionresults.size() > 0) {
								executionresults.each { result ->
									def resultAttrList = getTestItemAttributes(result)
									insertArtifactIntoExcel("${tc.@href}", 'Test Result', resultAttrList)
								}
							}
						}
						// Close out 
						excelManagementService.CloseExcelFile()
					}	

				}


				if (phase == 'qmAttachments') {
					//def linkMapping = processTemplateService.getLinkMapping(mapping)
					ChangeListManager clManager = new ChangeListManager(collection, tfsProject, workManagementService )
					def idKeyMap = [:]
					clmTestItemManagementService.resetNewId()
					items.each { testItem ->
						def testplan = clmTestManagementService.getTestItem(testItem.id.text())
						//String itemXml = XmlUtil.serialize(testplan)
						String webId = "${testplan.webId.text()}"
						String parentHref = "${testItem.id.text()}"
						testplan.testsuite.each { testsuiteRef ->
							def testsuite = clmTestManagementService.getTestItem("${testsuiteRef.@href}")
							testsuite.testcase.each { testcaseRef ->
								def testcase = clmTestManagementService.getTestItem("${testcaseRef.@href}")
								String idtype = "${testcase.webId.text()}-testcase"
								if (!idKeyMap.containsKey(idtype)) {
									String id = "${testcase.webId.text()}-Test Case"
									def files = clmAttachmentManagementService.cacheTestCaseAttachments(testcase)
									def wiChanges = fileManagementService.ensureAttachments(collection, tfsProject, id, files)
									if (wiChanges != null) {
										clManager.add(id, wiChanges)
									}
									idKeyMap[idtype] = idtype
								}
							}
						}
						testplan.testcase.each { testcaseRef ->
							def testcase = clmTestManagementService.getTestItem("${testcaseRef.@href}")
							String id = "${testcase.webId.text()}-Test Case"
							String idtype = "${testcase.webId.text()}-testcase"
							if (!idKeyMap.containsKey(idtype)) {
								def files = clmAttachmentManagementService.cacheTestCaseAttachments(testcase)
								def wiChanges = fileManagementService.ensureAttachments(collection, tfsProject, id, files)
								if (wiChanges != null) {
									clManager.add(id, wiChanges)
								}
								idKeyMap[idtype] = idtype
							}
						}
					}
					clManager.flush()

				}
			}
		}
		//notificationService.sendActionCompleteNotification('translateRQMToAdoForCore', phases)
		//extract & apply attachments.

		//ccmWorkManagementService.rtcRepositoryClient.shutdownPlatform()
	}
	def getTestPlanAttributes( def testplan ) {
		return getTestItemAttributes(testplan)
	}
	def getTestSuiteAttributes( def suiteHref ) {
		def attrMap = [:]
		def testsuite = clmTestManagementService.getTestItem(suiteHref)
		attrMap = getTestItemAttributes(testsuite)
		
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
	def getTestCaseAttributes( def testcase, def testscript, def prefix ) {
		def attrMap = [:]
		attrMap = getTestItemAttributes(testcase)
		
		// Now add script attributes
		if (testscript) {
			def scriptAttributes = getTestItemAttributes(testscript,prefix)
			attrMap = attrMap + scriptAttributes
		}
		// Cache file attachment(s)
		def files = clmAttachmentManagementService.cacheTestCaseAttachments(testcase)
		if (files.size() > 0) {
			String attrValue = ''
			String dir = "$archiveDir/Attachments"
			  files.each { file ->
				def fname = "${workItem.getId()}_${file.fileName}"
				archiveFile(fname, dir, file.file)
				if (attrValue != '') attrValue = attrValue + '\r'
				attrValue = attrValue + "$dir/$fname"
			}
			attrMap.put('Attachments',"$attrValue")
		}
		return attrMap
	}

	def getTestItemAttributes( def item, String prefix='') {
		def attrMap = [:]
		item.children().each {  node ->
			def key = "${node.name()}"
			def val
			
			if ( "${node.name()}" == 'scriptStepCount' ||
				"${node.name()}" == 'testscript' ||
				"${node.name()}" == 'template' ||
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
				if ( key == 'creator' || key == 'owner' || key == 'approvalOwner' || key == 'approval' || key == 'testedby') {
					def ref = childNode.attributes()
					// Get user email
					def userUrl = "${ref.'{http://www.w3.org/1999/02/22-rdf-syntax-ns#}resource'}"
					def userInfo = clmTestManagementService.getTestItem(userUrl)
					if (userInfo) val = "${userInfo.emailAddress.text()}".toLowerCase()
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
//				else if ( key == 'com.ibm.rqm.planning.editor.section.testCasePreCondition') {
//				}
				else if ( key == 'defect') {
					val = "${node.@rel} ${node.@summary}"
				}
				else if ( key == 'steps') {
					def iStep = 0
					node.children().each { step ->
						iStep++
						if (iStep != 1) return
						def description = step.description.div.text()
						def expectedResult = step.expectedResult.text()
						val = "$iStep - $description\nExpected Result: $expectedResult"
					}
					attrMap.put(prefix + 'stepCount', "$iStep")
				}
				else {
					val = "${node.toString()}"
					if ( key == 'com.ibm.rqm.planning.editor.section.testCasePreCondition' && val != '') {
						val = ''
						node.depthFirst().findAll({ "${it.name()}" == 'p' }).each { p ->
							p.children().each { pp ->
								val = val + "${pp.toString()}\n"
							}
						}
						def i
					}
				}
			}
			attrMap.put(prefix + key, val)
		}
		return attrMap
	}

	def getAdditionalSteps( def item, String prefix='') {
		def additionalSteps = []
		item.children().each {  node ->
			def key = "${node.name()}"
			def val
			
			if ( "${node.name()}" != 'steps' ) return
	
			def iStep = 0
			node.children().each { step ->
				iStep++
				if (iStep == 1) return  // only fetching steps 2+
				def description = step.description.div.text()
				def expectedResult = step.expectedResult.text()
				additionalSteps.add("$iStep - $description\nExpected Result: $expectedResult")
			}
		}
		return additionalSteps
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

	boolean planLinked(def plan) {
		String id = "${plan.webId.text()}-Test Suite"
		def adoSuite = cacheManagementService.getFromCache(id, 'suiteData')
		if (!adoSuite) return false

		def adoTCMap = testManagementService.getSuiteTestCaseMap(adoSuite.url)
		int tcSize = 0
		if (plan.testcase) {
			tcSize = plan.testcase.size()
		}
		return tcSize == adoTCMap.size()
	}

	boolean suiteLinked(def suite) {
		String id = "${suite.webId.text()}-Inner Test Suite"
		def adoSuite = cacheManagementService.getFromCache(id, 'suiteData')
		if (!adoSuite) return false

		def adoTCMap = testManagementService.getSuiteTestCaseMap(adoSuite.url)
		def tcList = []

		suite.suiteelements.suiteelement.each { suiteelement ->
			if (suiteelement.testcase) {
				tcList.add(suiteelement.testcase)
			}
		}


		return tcList.size() == adoTCMap.size()
	}

	def getLastResult(executionresults) {
		def rs = []
		if (executionresults.size() > 1) {
			def ro = null
			Date rDate = null
			executionresults = executionresults.each { r ->
				String dStr = "${r.updated.text()}"
				Date d = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", dStr)
				if (rDate == null || rDate.time <= d.time) {
					rDate = d
					ro = r
				}
			}
			rs.add(ro)
		} else if (executionresults.size() == 1){
			rs.add(executionresults.get(0))
		}
		return rs
	}


	def getRemainderResults(executionresults) {
		def rs = []
		def l = getLastResult(executionresults)
		def lr = l[0]
		if (executionresults.size() > 1) {
			def ro = null
			String lrWebId = "${lr.webId.text()}"
			rs = executionresults.findAll { r ->
				String dWebId = "${r.webId.text()}"
				dWebId != lrWebId
			}
		}
		return rs
	}

	def loadTestTypes(def templateDir) {
		def testTypes = []
		File tDir = new File(templateDir)
		if (tDir.exists() || tDir.isDirectory()) {
			tDir.eachFile { file ->
				def ttData = new XmlSlurper().parse(file)
				testTypes.add(ttData)
			}
		}
		return testTypes
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

}

