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
 * @author z091182
 * 
 * @startuml
 * 
 * annotation Autowired
 * 
 * class Map<? extends String, ? extends IFilter> {
 * 	+ put(key, element)
 * 	+ get(key)
 * .. groovy access/set elements ...
 * 	+ [key] 
 * }
 * class TranslateRQMToMTM {
 * 	+validate(ApplicationArguments args)
 * 	+execute(ApplicationArguments args)
 * 	+filtered(def, String)
 * }
 * 
 * CliAction <|-- TranslateRQMToMTM
 * TranslateRQMToMTM .. Autowired:  Spring Boot
 * TranslateRQMToMTM o--> Map: @Autowired filterMap
 * TranslateRQMToMTM o--> QmMetadataManagementService: @Autowired qmMetadataManagementService
 * 
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

	public TranslateRQMToMTM() {
	}

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
				filtered.each { testItem ->
					def testplan = clmTestManagementService.getTestItem(testItem.id.text())
					String itemXml = XmlUtil.serialize(testplan)
					int id = Integer.parseInt(testplan.webId.text())
					
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
							def tschanges = clmTestItemManagementService.getChanges(tfsProject, testsuite, memberMap, plan)
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
						String tcXml = XmlUtil.serialize(testcase)
						int aid = Integer.parseInt(testcase.webId.text())
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
					int bcount = 0
					def bidMap = [:]
					def bchangeList = []
					int tcount = 0
					while (tcount < count) {
						bidMap[bcount] = idMap[tcount]
						bchangeList.add(changeList[tcount])
						bcount++
						if (bcount == 200) {
							workManagementService.batchWIChanges(collection, tfsProject, bchangeList, bidMap)
							bcount = 0
							bidMap = [:]
							bchangeList = []
						}
						tcount++
					}
					if (bcount > 0) {
						workManagementService.batchWIChanges(collection, tfsProject, bchangeList, bidMap)
						
					}
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
			//def linkMapping = processTemplateService.getLinkMapping(mapping)
			def testItems = clmTestManagementService.getTestPlansViaQuery(wiQuery, project)
			while (true) {
				def changeList = []
				def idMap = [:]
				int count = 0
				def filtered = filtered(testItems, wiFilter)
				filtered.each { testItem ->
					def testplan = clmTestManagementService.getTestItem()
					String itemXml = XmlUtil.serialize(testplan)
					String webId = "${testplan.webId.text()}"
					String parentHref = "${testItem.id.text()}"
					def testRun = testManagementService.ensureTestRun(collection, tfsProject, testplan)
					testplan.testsuite.each { testsuiteRef ->
						def testsuite = clmTestManagementService.getTestItem("${testsuiteRef.@href}")
						def tcs = []
						testsuite.testcase.each { testcaseRef ->
							def testcase = clmTestManagementService.getTestItem("${testcaseRef.@href}")
							tcs.add(testcase)
						}
					}
					def tcs = []
					testplan.testcase.each { testcaseRef ->
						def testcase = clmTestManagementService.getTestItem("${testcaseRef.@href}")
						String tcitemXml = XmlUtil.serialize(testcase)
						String tcwebId = "${testcase.webId.text()}"
						tcs.add(testcase)
					}
				}
				def nextLink = testItems.'**'.find { node ->
					
					node.name() == 'link' && node.@rel == 'next'
				}
				if (nextLink == null) break
				testItems = clmTestManagementService.nextPage(nextLink.@href)
			}
		}

		//extract & apply attachments.
//		if (includes['attachments'] != null) {
//			def linkMapping = processTemplateService.getLinkMapping(mapping)
//			def workItems = clmWorkItemManagementService.getWorkItemsViaQuery(wiQuery)
//			while (true) {
//				def changeList = []
//				def idMap = [:]
//				int count = 0
//				def filtered = filtered(workItems, wiFilter)
//				filtered.each { workitem ->
//					int id = Integer.parseInt(workitem.id.text())
//					def files = attachmentsManagementService.cacheWorkItemAttachments(id)
//					def wiChanges = fileManagementService.ensureAttachments(collection, tfsProject, id, files)
//					if (wiChanges != null) {
//						idMap[count] = "${id}"
//						changeList.add(wiChanges)
//						count++
//					}
//				}
//				if (changeList.size() > 0) {
//					workManagementService.batchWIChanges(collection, tfsProject, changeList, idMap)
//				}
//				def rel = workItems.@rel
//				if ("${rel}" != 'next') break
//					workItems = clmWorkItemManagementService.nextPage(workItems.@href)
//			}
//		}

		//ccmWorkManagementService.rtcRepositoryClient.shutdownPlatform()
	}

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
