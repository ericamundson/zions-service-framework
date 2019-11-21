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
import com.zions.common.services.logging.FlowInterceptor
import com.zions.common.services.query.IFilter
import com.zions.common.services.restart.IRestartManagementService
import com.zions.qm.services.test.ClmTestAttachmentManagementService
import com.zions.qm.services.test.ClmTestItemManagementService
import com.zions.qm.services.test.ClmTestManagementService
import com.zions.qm.services.metadata.QmMetadataManagementService
import com.zions.qm.services.test.TestMappingManagementService
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
class TranslateRQMToADOForCore implements CliAction {
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

	@Value('${process.full.plan:false}')
	String processFullPlan

	@Value('${refresh.run:false}')
	boolean refreshRun

	@Value('${update.links:false}')
	boolean updateLinks

	@Value('${parent.plan.name:}')
	String parentPlanName

	@Autowired
	ICacheManagementService cacheManagementService

	public TranslateRQMToADOForCore() {
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
		if (includes['cleanCache'] != null) {
			cacheManagementService.clear()
			//def updated = processTemplateService.updateWorkitemTemplates(collection, tfsProject, mapping, testTypes)
		}
		if (includes['refresh'] != null) {
			workManagementService.refreshCacheByTeamArea(collection, tfsProject, areaPath)
		}
		if (includes['flushQueries'] != null) {
			clmTestManagementService.flushQueries(project)
			//def updated = processTemplateService.updateWorkitemTemplates(collection, tfsProject, mapping, testTypes)
		}
		def mappingData = testMappingManagementService.mappingData
		def memberMap = memberManagementService.getProjectMembersMap(collection, tfsProject)
		def parentPlan = testManagementService.getPlan(collection, tfsProject, parentPlanName)
		if (includes['phases'] != null) {
			restartManagementService.processPhases { phase, items ->
				//translate test platform configurations.
				if (phase == 'configurations') {
					items.each { testItem ->
						def configuration = clmTestManagementService.getTestItem(testItem.id.text())
						//int id = Integer.parseInt(configuration.webId.text())
						def id = "${configuration.name.text()}"
						clmTestItemManagementService.processForChanges(tfsProject, configuration, memberMap) { key, val ->
							def oconfig = testManagementService.sendPlanChanges(collection, tfsProject, val, "${id}-{key}")
						}
					}
				}
				//translate test case work data.
				if (phase == 'testcase') {
					ChangeListManager clManager = new ChangeListManager(collection, tfsProject, workManagementService )
					def idKeyMap = [:]
					clmTestItemManagementService.resetNewId()
					items.each { testItem ->
						def testcase = clmTestManagementService.getTestItem(testItem.id.text())
						int aid = Integer.parseInt(testcase.webId.text())
						clmTestItemManagementService.cacheLinkChanges(testcase)
						String idtype = "${aid}-testcase"
						if (!idKeyMap.containsKey(idtype)) {
							clmTestItemManagementService.processForChanges(tfsProject, testcase, memberMap) { key, val ->
								def files = clmAttachmentManagementService.cacheTestCaseAttachments(testcase)
								val = fileManagementService.ensureAttachments(collection, tfsProject, "${aid}-${key}", files, val)
								if (val) {
									clManager.add("${aid}-${key}", val)
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
					clmTestItemManagementService.resetNewId()
					items.each { testItem ->
						def testplan = clmTestManagementService.getTestItem(testItem.id.text())
						int id = Integer.parseInt(testplan.webId.text())
						clmTestItemManagementService.cacheLinkChanges(testplan)
						def psuite = null
						clmTestItemManagementService.processForChanges(tfsProject, testplan, memberMap, null, null, parentPlan) { String key, def val ->
							if (key.endsWith('WI')) {
								clManager.add("${id}-${key}", val)
							} else {
								psuite = testManagementService.sendPlanChanges(collection, tfsProject, val, "${id}-${key}")
							}
						}

						testplan.testsuite.each { testsuiteRef ->
							def testsuite = clmTestManagementService.getTestItem("${testsuiteRef.@href}")
							//println new XmlUtil().serialize(testsuite)
							clmTestItemManagementService.cacheLinkChanges(testsuite)
							//String tsXml = XmlUtil.serialize(testsuite)
							int tsid = Integer.parseInt(testsuite.webId.text())
							String idtype = "${tsid}-testsuite"
							if (!idKeyMap.containsKey(idtype)) {
								clmTestItemManagementService.processForChanges(tfsProject, testsuite, memberMap, null, null, psuite) { key, val ->
									if (key.endsWith(' WI')) {
										clManager.add("${tsid}-${key}", val)
									} else {
										String idkey = "${tsid}-${key}"
										def suite = testManagementService.sendPlanChanges(collection, tfsProject, val, "${idkey}")
									}
								}
								idKeyMap[idtype] = idtype
							}
							if (processFullPlan == 'true') {
								if (testsuite.suiteelements) {
									testsuite.suiteelements.suiteelement.each { suiteelement ->
										if (suiteelement.testcase) {
											def testcaseRef = suiteelement.testcase
											def testcase = clmTestManagementService.getTestItem("${testcaseRef.@href}")
											if (testcase) {
												clmTestItemManagementService.cacheLinkChanges(testcase)
												int aid = Integer.parseInt(testcase.webId.text())
												//generate test data
												idtype = "${aid}-testcase"
												if (!idKeyMap.containsKey(idtype)) {
													def tcchanges = clmTestItemManagementService.processForChanges(tfsProject, testcase, memberMap) { key, val ->
														String tid = "${aid}-${key}".toString()
														def files = clmAttachmentManagementService.cacheTestCaseAttachments(testcase)
														val = fileManagementService.ensureAttachments(collection, tfsProject, tid, files, val)
														if (val) {
															clManager.add("${aid}-${key}", val)
														}
													}
													idKeyMap[idtype] = idtype
												}
											} else {
												String url = "${testcaseRef.@href}"
												TranslateRQMToADOForCore.log.error("Failed to access test case via url:  ${url}")
											}
										}
									}
								}
							}
						}
						if (processFullPlan == 'true') {
							testplan.testcase.each { testcaseRef ->
								def testcase = clmTestManagementService.getTestItem("${testcaseRef.@href}")
								if (testcase) {
									int aid = Integer.parseInt(testcase.webId.text())
									clmTestItemManagementService.cacheLinkChanges(testcase)
									//generate test data
									String idtype = "${aid}-testcase"
									if (!idKeyMap.containsKey(idtype)) {
										def tcchanges = clmTestItemManagementService.processForChanges(tfsProject, testcase, memberMap) { key, val ->
											String tid = "${aid}-${key}".toString()
											def files = clmAttachmentManagementService.cacheTestCaseAttachments(testcase)
											val = fileManagementService.ensureAttachments(collection, tfsProject, tid, files, val)
											if (val) {
												clManager.add("${aid}-${key}", val)
											}
										}
										idKeyMap[idtype] = idtype
									}
								} else {
									String url = "${testcaseRef.@href}"
									TranslateRQMToADOForCore.log.error("Failed to access test case via url:  ${url}")
								}
							}
						}
					}
					clManager.flush();
				}
				//		workManagementService.testBatchWICreate(collection, tfsProject)
				//apply work links
				if (phase == 'links') {
					ChangeListManager clManager = new ChangeListManager(collection, tfsProject, workManagementService )
					def idKeyMap = [:]
					items.each { testItem ->
						def testplan = clmTestManagementService.getTestItem(testItem.id.text())
						String itemXml = XmlUtil.serialize(testplan)
						String webId = "${testplan.webId.text()}"
						String idtype = "${webId}-testplan"
						if (!idKeyMap.containsKey(idtype)) {
							clmTestItemManagementService.processForLinkChanges(testplan) { key, val ->
								String tid = "${webId}-${key}".toString()
								clManager.add(tid,val)
							}
							idKeyMap[idtype] = idtype
						}
						testplan.testsuite.each { testsuiteRef ->
							def testsuite = clmTestManagementService.getTestItem("${testsuiteRef.@href}")
							def tcs = []
							String tswebId = "${testsuite.webId.text()}"
							idtype = "${tswebId}-testsuite"
							if (!idKeyMap.containsKey(idtype)) {
								clmTestItemManagementService.processForLinkChanges(testsuite) { key, val ->
									String tid = "${tswebId}-${key}".toString()
									clManager.add(tid,val)
								}
								idKeyMap[idtype] = idtype
							}
							if (testsuite.suiteelements) {
								def atcs = []
								testsuite.suiteelements.suiteelement.each { suiteelement ->
									if (suiteelement.testcase) {
										def testcaseRef = suiteelement.testcase
										def testcase = clmTestManagementService.getTestItem("${testcaseRef.@href}")
										if (testcase) {
											atcs.add(testcase)
											String tcwebId = "${testcase.webId.text()}"
											idtype = "${tcwebId}-testcase"
											if (!idKeyMap.containsKey(idtype)) {
												clmTestItemManagementService.processForLinkChanges(testcase) { key, val ->
													String tid = "${tcwebId}-${key}".toString()
													clManager.add(tid,val)
												}
												idKeyMap[idtype] = idtype
											}
										} else {
											String url = "${testcaseRef.@href}"
											TranslateRQMToADOForCore.log.error("Failed to access test case via url:  ${url}")
										}
									}
								}
								testManagementService.setParent(testsuite, atcs, mappingData, updateLinks)
							}
						}
						def tcs = []
						testplan.testcase.each { testcaseRef ->
							def testcase = clmTestManagementService.getTestItem("${testcaseRef.@href}")
							if (testcase) {
								String tcitemXml = XmlUtil.serialize(testcase)
								String tcwebId = "${testcase.webId.text()}"
								tcs.add(testcase)
								idtype = "${tcwebId}-testcase"
								if (!idKeyMap.containsKey(idtype)) {
									clmTestItemManagementService.processForLinkChanges(testcase) { key, val ->
										String tid = "${tcwebId}-${key}".toString()
										clManager.add(tid,val)
									}
									idKeyMap[idtype] = idtype
								}
							} else {
								String url = "${testcaseRef.@href}"
								TranslateRQMToADOForCore.log.error("Failed to access test case via url:  ${url}")
							}
						}
						testManagementService.setParent(testplan, tcs, mappingData, updateLinks)
					}
					clManager.flush()
				}

				if (phase == 'executions') {
					//def linkMapping = processTemplateService.getLinkMapping(mapping)
					items.each { testItem ->
						def testplan = clmTestManagementService.getTestItem(testItem.id.text())
						//String itemXml = XmlUtil.serialize(testplan)
						String webId = "${testplan.webId.text()}"
						String parentHref = "${testItem.id.text()}"
						String planIdentifier = "${testplan.identifier.text()}"
						def resultMap = testManagementService.ensureTestRunForTestSuite(collection, tfsProject, testplan, refreshRun)
						testplan.testsuite.each { testsuiteRef ->
							def testsuite = clmTestManagementService.getTestItem("${testsuiteRef.@href}")
							if (testsuite.suiteelements) {
								String suitewebId = "${testsuite.webId.text()}"
								def atcs = []
								def subResultMap = testManagementService.ensureTestRunForTestSuite(collection, tfsProject, testsuite, refreshRun)
								testsuite.suiteelements.suiteelement.each { suiteelement ->
									if (suiteelement.testcase) {
										def testcaseRef = suiteelement.testcase
										def testcase = clmTestManagementService.getTestItem("${testcaseRef.@href}")
										if (testcase) {
											String tcwebId = "${testcase.webId.text()}"
											String tcIdentifier = "${testcase.identifier.text()}"
											//def resultMap = testManagementService.ensureTestRun(collection, tfsProject, testplan, testcase)
											def executionresults = clmTestManagementService.getExecutionResultViaHref(tcwebId, webId, project)
											executionresults = getLastResult(executionresults)
											if (executionresults.size() > 0) {
												executionresults.each { result ->
													clmTestItemManagementService.processForChanges(tfsProject, result, memberMap, subResultMap, testcase) { key, resultData ->
														String rwebId = "${result.webId.text()}-${key}"
														testManagementService.sendResultChanges(collection, tfsProject, resultData, rwebId)
													}
												}
											} else {
												String rqmId = "${testcase.webId.text()}-Test Case"
												def adoTestCase = cacheManagementService.getFromCache(rqmId, ICacheManagementService.WI_DATA)
												if (adoTestCase) {
													testManagementService.resetToActive(adoTestCase, subResultMap)
												}
											}
										} else {
											String url = "${testcaseRef.@href}"
											TranslateRQMToADOForCore.log.error("Failed to access test case via url:  ${url}")
										}
									}
								}
							}
						}
						testplan.testcase.each { testcaseRef ->
							def testcase = clmTestManagementService.getTestItem("${testcaseRef.@href}")
							if (testcase) {
								//String tcitemXml = XmlUtil.serialize(testcase)
								String tcwebId = "${testcase.webId.text()}"
								String tcIdentifier = "${testcase.identifier.text()}"
								//def resultMap = testManagementService.ensureTestRun(collection, tfsProject, testplan, testcase)
								def executionresults = clmTestManagementService.getExecutionResultViaHref(tcwebId, webId, project)
								executionresults = getLastResult(executionresults)
								if (executionresults.size() > 0) {
									executionresults.each { result ->
										clmTestItemManagementService.processForChanges(tfsProject, result, memberMap, resultMap, testcase) { key, resultData ->
											String rwebId = "${result.webId.text()}-${key}"
											testManagementService.sendResultChanges(collection, tfsProject, resultData, rwebId)
										}
									}
								} else {
									String rqmId = "${testcase.webId.text()}-Test Case"
									def adoTestCase = cacheManagementService.getFromCache(rqmId, ICacheManagementService.WI_DATA)
									if (adoTestCase) {
										testManagementService.resetToActive(adoTestCase, resultMap)
									}
								}

							} else {
								String url = "${testcaseRef.@href}"
								TranslateRQMToADOForCore.log.error("Failed to access test case via url:  ${url}")
							}
						}
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
		//extract & apply attachments.

		//ccmWorkManagementService.rtcRepositoryClient.shutdownPlatform()
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



}
