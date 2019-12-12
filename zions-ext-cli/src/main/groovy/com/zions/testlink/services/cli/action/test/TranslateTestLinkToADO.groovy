package com.zions.testlink.services.cli.action.test

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import br.eti.kinoshita.testlinkjavaapi.model.Execution
import br.eti.kinoshita.testlinkjavaapi.model.TestCase
import br.eti.kinoshita.testlinkjavaapi.model.TestPlan
import br.eti.kinoshita.testlinkjavaapi.model.TestSuite
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.logging.FlowInterceptor
import com.zions.common.services.logging.Traceable
import com.zions.common.services.query.IFilter
import com.zions.common.services.restart.IRestartManagementService
import com.zions.testlink.services.test.TestLinkAttachmentManagementService
import com.zions.testlink.services.test.TestLinkClient
import com.zions.testlink.services.test.TestLinkItemManagementService
import com.zions.testlink.services.test.TestLinkMappingManagementService
import com.zions.vsts.services.admin.member.MemberManagementService
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
 * 	<li>TranslateTestLinkToADO - The action's Spring bean name.</li>
 * <ul>
 * <p><b>The following's command-line format: --name=value</b></p>
 * <ul>
 *  <li>tfs.url - ADO url</li>
 *  <li>tfs.user - ADO user</li>
 *  <li>tfs.token - ADO PAT</li>
 *  <li>tfs.project - ADO project</li>
 *  <li>tl.template.dir - TestLink meta-data xml</li>
 *  <li>tfs.areapath - ADO area path to set Test planning items.</li>
 *  <li>test.mapping.file - The xml mapping file to enable field data flow.</li>
 *  <li>tl.filter - the name of filter class to used to pair down items that can't be filtered by query.</li>
 *  <li>include.update - Comma delimited list of the phases that will run during execution. E.G. refresh,clean,data,execution,links,attachments</li>
 *  </ul>
 * </ul>
 * 
 * <p><b>Design:</b></p>
 * <img src="TranslateTestLinkToADO_class_diagram.png"/>
 * <p><b>Flow:</b></p>
 * <img src="TranslateTestLinkToADO_sequence_diagram.png"/>
 * @author z091182
 * 
 * @startuml TranslateTestLinkToADO_class_diagram.png
 * 
 * annotation Autowired
 * annotation Component
 * class Map<? extends String, ? extends IFilter> {
 * 	+ put(key, element)
 * 	+ get(key)
 * .. groovy access/set elements ...
 * 	+ [key] 
 * }
 * class TranslateTestLinkToADO {
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
 * CliAction <|.. TranslateTestLinkToADO
 * TranslateTestLinkToADO .. Autowired:  Spring Boot
 * TranslateTestLinkToADO .. Component: Spring injectable type
 * TranslateTestLinkToADO --> Map: @Autowired filterMap
 * package com.zions.vsts.services.work {
 *  TranslateTestLinkToADO --> WorkManagementService: @Autowired workManagementService
 *  TranslateTestLinkToADO --> FileManagementService: @Autowired fileManagementService
 *  TranslateTestLinkToADO --> ChangeListManager: clManager
 * }
 * TranslateTestLinkToADO --> com.zions.qm.services.metadata.QmMetadataManagementService: @Autowired qmMetadataManagementService
 * package com.zions.testlink.services.test {
 *  TranslateTestLinkToADO --> TestLinkItemManagementService: @Autowired testLinkManagementService
 *  TranslateTestLinkToADO --> TestLinkClient: @Autowired clmTestItemManagementService
 *  TranslateTestLinkToADO --> TestLinkMappingManagementService: @Autowired testLinkMappingManagementService
 * }
 * TranslateTestLinkToADO -->  com.zions.vsts.services.admin.member.MemberManagementService: @Autowired memberManagementService
 * TranslateTestLinkToADO --> com.zions.vsts.services.test.TestManagementService: @Autowired TestManagementService
 * TranslateTestLinkToADO --> com.zions.common.services.restart.IRestartManagementService: @Autowired restartManagementService
 * @enduml
 * 
 * @startuml TranslateTestLinkToADO_sequence_diagram.png
 * participant CliApplication
 * CliApplication -> TranslateTestLinkToADO: validate(ApplicationArguments args)
 * CliApplication -> TranslateTestLinkToADO: execute(ApplicationArguments args)
 * alt include.update has 'clean'
 * 	TranslateTestLinkToADO -> TestManagementService: cleanupTestItems(collection, tfsProject, areaPath)
 * end
 *  TranslateTestLinkToADO -> TestLinkMappingManagementService: get field mapping
 *  TranslateTestLinkToADO -> MemberManagementService: get member map
 * TranslateTestLinkToADO -> "IRestartManagementService:restartManagementService" as restartManagementService: processPhases
 * group restartManagmentService.processPhases closure phase, items ->
 *  alt phase == 'configuration'
 *  	loop each { configuration }
 *  		TranslateTestLinkToADO -> TestLinkClient: get configuration details
 *  		TranslateTestLinkToADO -> TestLinkItemManagementService: get data changes
 *  		TranslateTestLinkToADO -> TestManagementService: send configuration changes
 *  	end
 *  end
 *  alt phase == 'testcase'
 *  	loop each test case
 *  		TranslateTestLinkToADO -> TestLinkItemManagementService: get work item data changes
 *  		TranslateTestLinkToADO -> "ChangeListManager:clManager": add 'Test Case' work item changes
 *  	end
 *  TranslateTestLinkToADO -> "ChangeListManager:clManager": flush.
 *  end
 *  alt phase == 'plan'
 *  loop each { testplan object structure }
 *  	TranslateTestLinkToADO -> TestLinkItemManagementService: get data changes
 *  	TranslateTestLinkToADO -> TestManagementService: sent plan and suite changes
 *  	loop each test suite of test plan
 *  		TranslateTestLinkToADO -> TestLinkItemManagementService: get data changes
 *  		TranslateTestLinkToADO -> TestManagementService: sent plan and suite changes
 *  	end
 *  end
 *  end
 *  alt phase == 'links'
 *  loop each { testplan object structure }
 *  	loop each testsuite for testplan
 *  		loop each test case for test suite
 *  			TranslateTestLinkToADO -> List: add test case to list
 *  		end
 *  		TranslateTestLinkToADO -> TestManagementService: setParent of list of test case to test suite
 *  	end
 *  	loop each test case for test plan
 *  		TranslateTestLinkToADO -> List: add test case to list
 *  	end
 *  	TranslateTestLinkToADO -> TestManagementService: setParent of list of test case to test plan
 *  end
 *  end
 *  alt phase == 'execution'
 *  loop each { testplan object structure }
 *  	TranslateTestLinkToADO -> TestManagmentService: ensure test runs for test plan
 *  	loop each { test suite }
 *  		loop each { test case of test suite}
 *  			TranslateTestLinkToADO -> TestLinkItemManagementService: get execution results for test case of this suite
 *  			loop each execution result
 *  				TranslateTestLinkToADO -> TestLinkItemManagementService: ensure test result data for testcase
 *  				TranslateTestLinkToADO -> TestManagmentService: send test result data
 *  			end
 *  		end
 *  	end
 *  	loop each { test case of test plan}
 *  		TranslateTestLinkToADO -> TestLinkClient: get execution results for test case of this plan
 *  		loop each execution result
 TranslateTestLinkToADO -> TestLinkItemManagementService: ensure test result data for testcase
 *  			TranslateTestLinkToADO -> TestManagmentService: send test result data
 *  		end
 *  	end
 *  end
 *  end
 *  alt phase == 'attachments'
 *  	TranslateTestLinkToADO -> MemberManagementService: get member map
 *  	TranslateTestLinkToADO -> TestLinkClient: get test plans via query
 *  	loop each { test plans }
 *  		loop each test suite
 *  			loop each test case
 *  				TranslateTestLinkToADO -> TestLinkClient: get attachment changes for test case
 *  				
 *  				TranslateTestLinkToADO -> "ChangeListManager:clManager": add attachment changes 		
 *  			end
 *  		end
 *  	end
 *  	TranslateTestLinkToADO -> "ChangeListManager:clManager":flush.
 *  end
 *  end
 * @enduml
 *
 */
@Component
@Slf4j
@Traceable
class TranslateTestLinkToADO implements CliAction {
	@Autowired
	MemberManagementService memberManagementService;
	//	@Autowired
	//	AttachmentsManagementService attachmentsManagementService
	@Autowired
	FileManagementService fileManagementService;
	@Autowired
	TestManagementService testManagementService;

	@Autowired
	IRestartManagementService restartManagementService
	
	@Autowired
	TestLinkMappingManagementService testLinkMappingManagementService
	
	@Autowired
	TestLinkItemManagementService testLinkItemManagementService
	@Autowired
	TestLinkAttachmentManagementService testLinkAttachmentManagementService

	@Autowired
	WorkManagementService workManagementService
	
	@Autowired
	ICacheManagementService cacheManagementService
	
	@Autowired
	TestLinkClient testLinkClient

	@Value('${process.full.plan:false}')
	boolean processFullPlan
	
	@Value('${update.links:false}')
	boolean updateLinks
	

	public TranslateTestLinkToADO() {
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
		String mappingFile = data.getOptionValues('test.mapping.file')[0]
		String tlQuery = data.getOptionValues('tl.query')[0]
		//String wiFilter = data.getOptionValues('tl.filter')[0]
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		String tfsProject = data.getOptionValues('tfs.project')[0]
		File mFile = new File(mappingFile)

		def mapping = new XmlSlurper().parseText(mFile.text)
		//Update TFS wit definitions.
		if (includes['meta'] != null) {
			//def updated = processTemplateService.updateWorkitemTemplates(collection, tfsProject, mapping, testTypes)
		}
		if (includes['clean'] != null) {
			new FlowInterceptor() {}.flowLogging(testManagementService) {
				testManagementService.cleanupTestItems(collection, tfsProject, areaPath)
			}
			//def updated = processTemplateService.updateWorkitemTemplates(collection, tfsProject, mapping, testTypes)
		}
		if (includes['cleanplans'] != null) {
			new FlowInterceptor() {}.flowLogging(testManagementService) {
				String query = "Select [System.Id], [System.Title] From WorkItems Where ([System.WorkItemType] = 'Test Plan') AND [System.AreaPath] = '${areaPath}' AND [Custom.ExternalID] <> ''"
				testManagementService.cleanupTestItems(collection, tfsProject, areaPath, query)
			}
			//def updated = processTemplateService.updateWorkitemTemplates(collection, tfsProject, mapping, testTypes)
		}
		if (includes['refresh'] != null) {
			String query = "Select [System.Id], [System.Title] From WorkItems Where ([System.AreaPath] under '${areaPath}')"
			workManagementService.refreshCacheByQuery(collection, tfsProject, query)
			//def updated = processTemplateService.updateWorkitemTemplates(collection, tfsProject, mapping, testTypes)
		}
		def mappingData = testLinkMappingManagementService.mappingData
		def memberMap = memberManagementService.getProjectMembersMap(collection, tfsProject)
		if (includes['phases'] != null) {
			restartManagementService.processPhases { phase, items ->
				//translate test platform configurations.
				if (phase == 'configurations') {
					items.each { testItem ->
						def configuration = testLinkClient.getTestItem(testItem.id.text())
						//int id = Integer.parseInt(configuration.webId.text())
						def id = "${configuration.name.text()}"
						testLinkItemManagementService.processForChanges(tfsProject, configuration, memberMap) { key, val ->
							def oconfig = testManagementService.sendPlanChanges(collection, tfsProject, val, "${id}-{key}")
						}
					}
				}
				//translate test case work data.
				if (phase == 'testcase') {
					ChangeListManager clManager = new ChangeListManager(collection, tfsProject, workManagementService )
					def idKeyMap = [:]
					testLinkItemManagementService.resetNewId()
					items.each { testItem ->
						TestCase testcasea = testItem
						TestCase testcase = null
						try {
							testcase = testLinkClient.getTestCase(testcasea.id, null, null)
						} catch (e) {}
						if (!testcase) 
						{
							testcase = testcasea
						}
						int aid = testcase.id
						String idtype = "${aid}-testcase"
						if (!idKeyMap.containsKey(idtype)) {
							testLinkItemManagementService.processForChanges(tfsProject, testcase, memberMap) { key, val ->
								def files = testLinkAttachmentManagementService.cacheTestCaseAttachments(testcasea)
								String tid = "${aid}".toString()
								val = fileManagementService.ensureAttachments(collection, tfsProject, tid, files, val)
								if (val) {
									clManager.add(tid,val)
								}
							}
							idKeyMap[idtype] = idtype
						}

					}
					clManager.flush();
					//translate work data.
				}
				if (phase == 'plans') {
					ChangeListManager clManager = new ChangeListManager(collection, tfsProject, workManagementService )
					def idKeyMap = [:]
					testLinkItemManagementService.resetNewId()
					items.each { testItem ->
						TestPlan testplan = testItem
						int pid = testplan.id


						def testcaseCallback = null
						if (processFullPlan) {
							testcaseCallback = { TestSuite psuite, TestCase testcasea -> 
								TestCase testcase = null
								try {
									testcase = testLinkClient.getTestCase(testcasea.id, null, null)
								} catch (e) {}
								if (!testcase) 
								{
									testcase = testcasea
								}
								int aid = testcase.id
								 //generate test data
								String idtype = "${aid}-testcase"
								if (!idKeyMap.containsKey(idtype)) {
									def tcchanges = testLinkItemManagementService.processForChanges(tfsProject, testcase, memberMap) { key, val ->
										String tid = "${aid}".toString()
										def files = testLinkAttachmentManagementService.cacheTestCaseAttachments(testcasea)
										val = fileManagementService.ensureAttachments(collection, tfsProject, tid, files, val)
										if (val) {
											clManager.add(tid,val)
										}
									}
									idKeyMap[idtype] = idtype
								}
							}
						}
						
						def plan = null
						testLinkItemManagementService.processForChanges(tfsProject, testplan, memberMap) { String key, def val ->
							if (key.endsWith('WI')) {
								clManager.add("${pid}", val)
							} else {
								plan = testManagementService.sendPlanChanges(collection, tfsProject, val, "${pid}")
							}
						}
							
						TestSuite[] suites = testLinkClient.getFirstLevelSuitesForTestPlan(testplan)
						handleTestSuites(suites, plan, testplan, null, { def parent, TestSuite testsuite -> 
							String tsid = "${testsuite.id}_${testplan.name}"
							def suite = null;
							testLinkItemManagementService.processForChanges(tfsProject, testsuite, memberMap, null, null, parent) { key, val ->
								if (key.endsWith(' WI')) {
									String cid = "${tsid} WI"
									
									clManager.add("${cid}", val)
									clManager.flush();
								} else {
									String idkey = "${tsid}"
									suite = testManagementService.sendPlanChanges(collection, tfsProject, val, "${idkey}")
								}
							}
							return suite
						}, testcaseCallback)
					}
					clManager.flush();
				}
				//		workManagementService.testBatchWICreate(collection, tfsProject)
				//apply work links
				if (phase == 'links') {
					def idKeyMap = [:]
					items.each { TestPlan testplan ->
						String webId = "${testplan.id}"
						String planName = "${testplan.name}"
						def plan = cacheManagementService.getFromCache(webId, ICacheManagementService.PLAN_DATA)
						TestSuite[] suites = testLinkClient.getFirstLevelSuitesForTestPlan(testplan)
						handleTestSuites(suites, plan, testplan, null,
						 { def parent, TestSuite testsuite ->
							String tswebId = "${testsuite.id}_${testplan.name}"
							TestCase[] testcaseList = testLinkClient.getTestCasesForTestPlanSuite(testsuite.id, testplan.id, false, 'full')
							testLinkItemManagementService.setParent(testsuite, testcaseList, mappingData, planName) { typeData, tcIds ->
								testManagementService.associateCaseToSuite(typeData, tcIds, updateLinks)
							}
							def suiteData = cacheManagementService.getFromCache(tswebId, ICacheManagementService.SUITE_DATA)
							return suiteData
						}, null )
					}
				}

				if (phase == 'executions') {
					//def linkMapping = processTemplateService.getLinkMapping(mapping)
					items.each { TestPlan testplan ->
						String webId = "${testplan.id}"
						TestCase[] testcaseList = testLinkClient.getTestCasesForTestPlan(testplan.id, null, null, null, null, null, null, null, null, false)
						testcaseList.each { TestCase testcasea ->
							Execution result = testLinkClient.getLastExecutionResult(testplan.id, testcasea.id, null)
							if (!result) return
							TestCase testcase = null
							try {
								testcase = testLinkClient.getTestCase(testcasea.id, null, null)
							} catch (e) {}
							if (!testcase) 
							{
								testcase = testcasea
							}
							Map resultMap = testManagementService.ensureTestRunForTestCaseAndPlan(collection, tfsProject, testplan, testcase)
							testLinkItemManagementService.processForChanges(tfsProject, result, memberMap, resultMap, testcase) { key, resultData ->
								String rwebId = "${result.id}"
								testManagementService.sendResultChanges(collection, tfsProject, resultData, rwebId)
							}
						}
					}

				}

				if (phase == 'attachments') {
					//def linkMapping = processTemplateService.getLinkMapping(mapping)
					ChangeListManager clManager = new ChangeListManager(collection, tfsProject, workManagementService )
					def idKeyMap = [:]
					testLinkItemManagementService.resetNewId()
					items.each { TestCase testcasea ->
						TestCase testcase = null
						try {
							testcase = testLinkClient.getTestCase(testcasea.id, null, null)
						} catch (e) {}
						if (!testcase) 
						{
							testcase = testcasea
						}
						String id = "${testcase.id}"
						def files = testLinkAttachmentManagementService.cacheTestCaseAttachments(testcasea)
						def wiChanges = fileManagementService.ensureAttachments(collection, tfsProject, id, files)
						if (wiChanges != null) {
							clManager.add(id, wiChanges)
						}
					}
					clManager.flush()
				}
			}
		}
		//extract & apply attachments.

		//ccmWorkManagementService.rtcRepositoryClient.shutdownPlatform()
	}
	
	def handleTestCaseForTestSuites(TestSuite suite, TestPlan plan, def resultMap = null, Closure c)
	{
		TestCase[] tcList = testLinkClient.getTestCasesForTestPlanSuite(suite.id, plan.id, false, 'full')
		tcList.each { TestCase tc ->
			c.call(suite, tc)
		}
	}
	
	def handleTestSuites(TestSuite[] psuites, def parent, TestPlan plan, def resultMap = null,  Closure tsc = null, Closure tcc = null) {
		psuites.each { TestSuite psuite ->
			def parentData = null
			if (tsc) {
				parentData = tsc.call(parent, psuite)
			}
			if (tcc) {
				handleTestCaseForTestSuites(psuite, plan, resultMap, tcc)
			}
			TestSuite[] csuites = testLinkClient.getSuitesForTestPlanSuites(plan.id, psuite.id)
			handleTestSuites(csuites, parentData, plan, resultMap, tsc, tcc)
		}
		
	}

	/**
	 * Filters top level queries items.
	 * 
	 * @param items - ojgect of elements to be filtered Groovy object generation from XML rest result
	 * @param filter - Name of IFilter to use
	 * @return filtered result.
	 */
//	def filtered(def items, String filter) {
//		if (this.filterMap[filter] != null) {
//			return this.filterMap[filter].filter(items)
//		}
//		return items.entry.findAll { ti -> true }
//	}


	/* (non-Javadoc)
	 * @see com.zions.common.services.cli.action.CliAction#validate(org.springframework.boot.ApplicationArguments)
	 */
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.project', 'tfs.areapath', 'test.mapping.file', 'tl.query']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}



}

