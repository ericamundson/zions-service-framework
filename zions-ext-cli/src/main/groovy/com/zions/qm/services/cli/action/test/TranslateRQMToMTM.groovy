package com.zions.qm.services.cli.action.test

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.clm.services.ccm.workitem.CcmWorkManagementService
import com.zions.clm.services.ccm.workitem.attachments.AttachmentsManagementService
import com.zions.clm.services.rtc.project.workitems.ClmWorkItemManagementService
import com.zions.clm.services.rtc.project.workitems.RtcWIMetadataManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.query.IFilter
import com.zions.qm.services.test.ClmTestAttachmentManagementService
import com.zions.qm.services.test.ClmTestItemManagementService
import com.zions.qm.services.test.ClmTestManagementService
import com.zions.qm.services.metadata.QmMetadataManagementService
import com.zions.qm.services.test.TestMappingManagementService
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.test.TestManagementService
import com.zions.vsts.services.work.FileManagementService
import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.work.templates.ProcessTemplateService
import groovy.json.JsonBuilder
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
 * @enduml
 * 
 * @startuml TranslateRQMToMTM_sequence_diagram.png
 * participant CliApplication
 * CliApplication -> TranslateRQMToMTM: validate(ApplicationArguments args)
 * CliApplication -> TranslateRQMToMTM: execute(ApplicationArguments args)
 * alt include.update has 'clean'
 * 	TranslateRQMToMTM -> TestManagementService: cleanupTestItems(collection, tfsProject, areaPath)
 * end
 *  alt include.update has 'data'
 *  TranslateRQMToMTM -> TestMappingManagementService: get field mapping
 *  TranslateRQMToMTM -> MemberManagementService: get member map
 *  TranslateRQMToMTM -> ClmTestManagementService: get test plans via query
 *  loop each { testplan object structure }
 *  	TranslateRQMToMTM -> ClmTestItemManagementService: get data changes
 *  	TranslateRQMToMTM -> TestManagementService: sent plan and suite changes
 *  	loop each test suite of test plan
 *  		TranslateRQMToMTM -> ClmTestItemManagementService: get data changes
 *  		TranslateRQMToMTM -> TestManagementService: sent plan and suite changes
 *  		loop each test case of test suite
 *  			TranslateRQMToMTM -> ClmTestItemManagementService: get work item data changes
 *  			TranslateRQMToMTM -> List: add 'Test Case' work item changes
 *  		end
 *  	end
 *  	loop each test case of test plan
 *  		TranslateRQMToMTM -> ClmTestItemManagementService: get work item data changes
 *  		TranslateRQMToMTM -> List: add 'Test Case' work item changes
 *  	end
 *  end
 *  TranslateRQMToMTM -> WorkManagementService: send list of changes to wi batch.
 *  end
 *  alt include.update has 'links'
 *  TranslateRQMToMTM -> TestMappingManagementService: get field mapping
 *  TranslateRQMToMTM -> MemberManagementService: get member map
 *  TranslateRQMToMTM -> ClmTestManagementService: get test plans via query
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
 *  alt include.update has 'execution'
 *  TranslateRQMToMTM -> TestMappingManagementService: get field mapping
 *  TranslateRQMToMTM -> MemberManagementService: get member map
 *  TranslateRQMToMTM -> ClmTestManagementService: get test plans via query
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
 *  alt include.update has 'attachments'
 *  	TranslateRQMToMTM -> MemberManagementService: get member map
 *  	TranslateRQMToMTM -> ClmTestManagementService: get test plans via query
 *  	loop each { test plans }
 *  		loop each test suite
 *  			loop each test case
 *  				TranslateRQMToMTM -> ClmTestItemManagementService: get attachment changes for test case
 *  						
 *  			end
 *  		end
 *  	end
 *  end
 *  alt include.update has 'configuration'
 *  	TranslateRQMToMTM -> MemberManagementService: get member map
 *  	TranslateRQMToMTM -> ClmTestManagementService: get test plans via query
 *  	loop each { test plans }
 *  	end
 *  end
 * @enduml
 *
 */
@Component
class TranslateRQMToMTM implements CliAction {
	@Autowired
	private Map<String, IFilter> filterMap;
	@Autowired
	QmMetadataManagementService qmMetadataManagementService;
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

	public TranslateRQMToMTM() {
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
		String wiQuery = data.getOptionValues('qm.query')[0]
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
			testManagementService.cleanupTestItems(collection, tfsProject, areaPath)
			//def updated = processTemplateService.updateWorkitemTemplates(collection, tfsProject, mapping, testTypes)
		}
		//refresh.
		if (includes['refresh'] != null) {
			def testItems = clmTestManagementService.getTestPlansViaQuery(wiQuery)
			while (true) {
				def changeList = []
				def filtered = filtered(testItems, wiFilter)
				filtered.each { testitem ->
					int id = Integer.parseInt(testitem.id.text())
					changeList.add(id)
				}
				def wiChanges = workManagementService.refreshCache(collection, tfsProject, changeList)
				def rel = testItems.@rel
				if ("${rel}" != 'next') break
					testItems = clmTestManagementService.nextPage(testItems.@href)
			}
		}
		//translate test platform configurations.
		if (includes['configurations'] != null) {
			def mappingData = testMappingManagementService.mappingData
			def configItems = clmTestManagementService.getConfigurationsViaQuery(wiQuery, project)
			def memberMap = memberManagementService.getProjectMembersMap(collection, tfsProject)
			while (true) {
				configItems.entry.each { testItem ->
					def configuration = clmTestManagementService.getTestItem(testItem.id.text())
					//int id = Integer.parseInt(configuration.webId.text())
					def id = "${configuration.name.text()}-Configuration"
//					String resultsxml = XmlUtil.serialize(configuration)
//					File resultFile = new File("../zions-ext-services/src/test/resources/testdata/configurationT.xml")
//					def os = resultFile.newDataOutputStream()
//					os << resultsxml
//					os.close()
					def changes = clmTestItemManagementService.getChanges(tfsProject, configuration, memberMap)
					changes.each { key, val ->
						def oconfig = testManagementService.sendPlanChanges(collection, tfsProject, val, id)
					}
				}
				def nextLink = configItems.'**'.find { node ->
					
					node.name() == 'link' && node.@rel == 'next'
				}
				if (nextLink == null) break
				configItems = clmTestManagementService.nextPage(nextLink.@href)
			}
		}
		//translate work data.
		if (includes['data'] != null) {
			def mappingData = testMappingManagementService.mappingData
			def testItems = clmTestManagementService.getTestPlansViaQuery(wiQuery, project)
			def memberMap = memberManagementService.getProjectMembersMap(collection, tfsProject)
			while (true) {
				//TODO: ccmWorkManagementService.resetNewId()
				def changeList = []
				def pidMap = [:]
				def idMap = [:]
				int count = 0
				int pcount = 0
				def pChangeList = []
				def idKeyMap = [:]
				def filtered = filtered(testItems, wiFilter)
				clmTestItemManagementService.resetNewId()
				filtered.each { testItem ->
					def testplan = clmTestManagementService.getTestItem(testItem.id.text())
					int id = Integer.parseInt(testplan.webId.text())
					//Generate test data
//					String itemXml = XmlUtil.serialize(testplan)
//					File resultFile = new File("../zions-ext-services/src/test/resources/testdata/testplan${id}.xml")
//					def os = resultFile.newDataOutputStream()
//					os << itemXml
//					os.close()
					
					
					def changes = clmTestItemManagementService.getChanges(tfsProject, testplan, memberMap)
					def plan = null
					changes.each { key, val ->
						plan = testManagementService.sendPlanChanges(collection, tfsProject, val, "${id}-${key}")
					}
					
					testplan.testsuite.each { testsuiteRef ->
						def testsuite = clmTestManagementService.getTestItem("${testsuiteRef.@href}")
						String tsXml = XmlUtil.serialize(testsuite)
						int tsid = Integer.parseInt(testsuite.webId.text())
						String idtype = "${tsid}-testsuite"
						if (!idKeyMap.containsKey(idtype)) {
							def tschanges = clmTestItemManagementService.getChanges(tfsProject, testsuite, memberMap, null, null, plan)
							tschanges.each { key, val ->
								String idkey = "${tsid}-${key}"
								def suite = testManagementService.sendPlanChanges(collection, tfsProject, val, "${id}-${key}")
							}
							idKeyMap[idtype] = idtype
						}
						testsuite.testcase.each { testcaseRef ->
							def testcase = clmTestManagementService.getTestItem("${testcaseRef.@href}")
							String tcXml = XmlUtil.serialize(testcase)
							int aid = Integer.parseInt(testcase.webId.text())
							String aidtype = "${aid}-testcase"
							if (!idKeyMap.containsKey(aidtype)) {
								def tcchanges = clmTestItemManagementService.getChanges(tfsProject, testcase, memberMap)
								tcchanges.each { key, val ->
									String idkey = "${aid}-${key}"
									idMap[count] = idkey
									changeList.add(val)
									count++
									
								}
								idKeyMap[aidtype] = aidtype
							}
						}	
					}
					testplan.testcase.each { testcaseRef ->
						def testcase = clmTestManagementService.getTestItem("${testcaseRef.@href}")
						int aid = Integer.parseInt(testcase.webId.text())
						// generate test data
//						String testcasexml = XmlUtil.serialize(testcase)
//						resultFile = new File("../zions-ext-services/src/test/resources/testdata/testcase${aid}.xml")
//						os = resultFile.newDataOutputStream()
//						os << testcasexml
//						os.close()
						String idtype = "${aid}-testcase"
						if (!idKeyMap.containsKey(idtype)) {
							def tcchanges = clmTestItemManagementService.getChanges(tfsProject, testcase, memberMap)
							tcchanges.each { key, val ->
								String idkey = "${aid}-${key}"
								idMap[count] = idkey
								changeList.add(val)
								count++
								
							}
							idKeyMap[idtype] = idtype
						}

					}
					// TODO: def wiChanges = ccmWorkManagementService.getWIChanges(id, tfsProject, translateMapping, memberMap)
//					if (wiChanges != null) {
//						idMap[count] = "${id}"
//						changeList.add(wiChanges)
//						count++
//					}
				}
				if (changeList.size() > 0) {
					workManagementService.batchWIChanges(collection, tfsProject, changeList, idMap)
				}
				def nextLink = testItems.'**'.find { node ->
					
					node.name() == 'link' && node.@rel == 'next'
				}
				if (nextLink == null) break
				testItems = clmTestManagementService.nextPage(nextLink.@href)
			}
		}
		//		workManagementService.testBatchWICreate(collection, tfsProject)
		//apply work links
		if (includes['links'] != null) {
			def itemMapping = testMappingManagementService.getMappingData()
			//def linkMapping = processTemplateService.getLinkMapping(mapping)
			def testItems = clmTestManagementService.getTestPlansViaQuery(wiQuery, project)
			while (true) {
				def changeList = []
				def idMap = [:]
				int count = 0
				def filtered = filtered(testItems, wiFilter)
				filtered.each { testItem ->
					def testplan = clmTestManagementService.getTestItem(testItem.id.text())
					String itemXml = XmlUtil.serialize(testplan)
					String webId = "${testplan.webId.text()}"
					testplan.testsuite.each { testsuiteRef ->
						def testsuite = clmTestManagementService.getTestItem("${testsuiteRef.@href}")
						def tcs = []
						testsuite.testcase.each { testcaseRef ->
							def testcase = clmTestManagementService.getTestItem("${testcaseRef.@href}")
							tcs.add(testcase)
						}
						testManagementService.setParent(testsuite, tcs, itemMapping)
					}
					def tcs = []
					testplan.testcase.each { testcaseRef ->
						def testcase = clmTestManagementService.getTestItem("${testcaseRef.@href}")
						String tcitemXml = XmlUtil.serialize(testcase)
						String tcwebId = "${testcase.webId.text()}"
						tcs.add(testcase)
					}
					testManagementService.setParent(testplan, tcs, itemMapping)
				}
				def nextLink = testItems.'**'.find { node ->
					
					node.name() == 'link' && node.@rel == 'next'
				}
				if (nextLink == null) break
				testItems = clmTestManagementService.nextPage(nextLink.@href)
			}
		}
		
		if (includes['execution'] != null) {
			def itemMapping = testMappingManagementService.getMappingData()
			def memberMap = memberManagementService.getProjectMembersMap(collection, tfsProject)
			//def linkMapping = processTemplateService.getLinkMapping(mapping)
			def testItems = clmTestManagementService.getTestPlansViaQuery(wiQuery, project)
			while (true) {
				def changeList = []
				def idMap = [:]
				int count = 0
				def filtered = filtered(testItems, wiFilter)
				filtered.each { testItem ->
					def testplan = clmTestManagementService.getTestItem(testItem.id.text())
					String itemXml = XmlUtil.serialize(testplan)
					String webId = "${testplan.webId.text()}"
					String parentHref = "${testItem.id.text()}"
					def resultMap = testManagementService.ensureTestRun(collection, tfsProject, testplan)
					testplan.testsuite.each { testsuiteRef ->
						def testsuite = clmTestManagementService.getTestItem("${testsuiteRef.@href}")
						testsuite.testcase.each { testcaseRef ->
							def testcase = clmTestManagementService.getTestItem("${testcaseRef.@href}")
							String tcwebId = "${testcase.webId.text()}"
							def executionresults = clmTestManagementService.getExecutionResultViaHref(tcwebId, webId, project)
							executionresults.each { result ->
								def resultData = clmTestItemManagementService.getChanges(tfsProject, result, memberMap, resultMap, testcase)
								String rwebId = "${result.webId.text()}-Result"
								testManagementService.sendResultChanges(collection, tfsProject, resultData, rwebId)
							}
						}
					}
					testplan.testcase.each { testcaseRef ->
						def testcase = clmTestManagementService.getTestItem("${testcaseRef.@href}")
						String tcitemXml = XmlUtil.serialize(testcase)
						String tcwebId = "${testcase.webId.text()}"
						def executionresults = clmTestManagementService.getExecutionResultViaHref(tcwebId, webId, project)
						executionresults.each { result ->
							def resultData = clmTestItemManagementService.getChanges(tfsProject, result, memberMap, resultMap, testcase)
							String rwebId = "${result.webId.text()}-Result"
							testManagementService.sendResultChanges(collection, tfsProject, resultData, rwebId)
						}
					}
				}
				def nextLink = testItems.'**'.find { node ->
					
					node.name() == 'link' && node.@rel == 'next'
				}
				if (nextLink == null) break
				testItems = clmTestManagementService.nextPage(nextLink.@href)
			}
		}

		if (includes['attachments'] != null) {
			def itemMapping = testMappingManagementService.getMappingData()
			def memberMap = memberManagementService.getProjectMembersMap(collection, tfsProject)
			//def linkMapping = processTemplateService.getLinkMapping(mapping)
			def testItems = clmTestManagementService.getTestPlansViaQuery(wiQuery, project)
			while (true) {
				def changeList = []
				def idMap = [:]
				int count = 0
				def filtered = filtered(testItems, wiFilter)
				filtered.each { testItem ->
					def testplan = clmTestManagementService.getTestItem(testItem.id.text())
					String itemXml = XmlUtil.serialize(testplan)
					String webId = "${testplan.webId.text()}"
					String parentHref = "${testItem.id.text()}"
					def resultMap = testManagementService.ensureTestRun(collection, tfsProject, testplan)
					testplan.testsuite.each { testsuiteRef ->
						def testsuite = clmTestManagementService.getTestItem("${testsuiteRef.@href}")
						testsuite.testcase.each { testcaseRef ->
							def testcase = clmTestManagementService.getTestItem("${testcaseRef.@href}")
							String id = "${testcase.webId.text()}-Test Case"
							def files = clmAttachmentManagementService.cacheTestCaseAttachments(testcase)
							def wiChanges = fileManagementService.ensureAttachments(collection, tfsProject, id, files)
							if (wiChanges != null) {
								idMap[count] = "${id}"
								changeList.add(wiChanges)
								count++
							}
						}
					}
					testplan.testcase.each { testcaseRef ->
						def testcase = clmTestManagementService.getTestItem("${testcaseRef.@href}")
						String id = "${testcase.webId.text()}-Test Case"
						def files = clmAttachmentManagementService.cacheTestCaseAttachments(testcase)		
						def wiChanges = fileManagementService.ensureAttachments(collection, tfsProject, id, files)
						if (wiChanges != null) {
							idMap[count] = "${id}"
							changeList.add(wiChanges)
							count++
						}
						String tcwebId = "${testcase.webId.text()}"
						def executionresults = clmTestManagementService.getExecutionResultViaHref(tcwebId, webId, project)
						executionresults.each { result ->
							def cacheData = []
							def rfiles = clmAttachmentManagementService.cacheTestItemAttachments(result)
							if (rfiles.size() > 0) {
								def attResult = testManagementService.ensureResultAttachments(collection, tfsProject, rfiles, testcase, resultMap)
							}	
						}
					}
				}
				if (changeList.size() > 0) {
					workManagementService.batchWIChanges(collection, tfsProject, changeList, idMap)
				}
				def nextLink = testItems.'**'.find { node ->
					
					node.name() == 'link' && node.@rel == 'next'
				}
				if (nextLink == null) break
				testItems = clmTestManagementService.nextPage(nextLink.@href)
			}
		}
		//extract & apply attachments.

		//ccmWorkManagementService.rtcRepositoryClient.shutdownPlatform()
	}

	/**
	 * Filters top level queries items.
	 * 
	 * @param items - ojgect of elements to be filtered Groovy object generation from XML rest result
	 * @param filter - Name of IFilter to use
	 * @return filtered result.
	 */
	def filtered(def items, String filter) {
		if (this.filterMap[filter] != null) {
			return this.filterMap[filter].filter(items)
		}
		return items.entry.findAll { ti ->
			true
		}
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



}
