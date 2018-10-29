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
import com.zions.qm.services.metadata.QmMetadataManagementService
import com.zions.qm.services.test.ClmTestManagementService
import com.zions.vsts.services.admin.member.MemberManagementService
import com.zions.vsts.services.work.FileManagementService
import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.work.templates.ProcessTemplateService
import groovy.json.JsonBuilder

/**
 * Provides command line interaction to synchronize RTC work items with VSTS.
 * 
 * @author z091182
 *
 */
@Component
class TranslateRQMToMTM implements CliAction {
	@Autowired
	private Map<String, IFilter> filterMap;
	@Autowired
	QmMetadataManagementService qmMetadataManagementService;
	@Autowired
	ProcessTemplateService processTemplateService;
	@Autowired
	WorkManagementService workManagementService;
	@Autowired
	ClmTestManagementService clmTestManagementService
	@Autowired
	MemberManagementService memberManagementService
	@Autowired
	AttachmentsManagementService attachmentsManagementService
	@Autowired
	FileManagementService fileManagementService

	public TranslateRQMToMTM() {
	}

	public def execute(ApplicationArguments data) {
		boolean excludeMetaUpdate = true
		def excludes = [:]
		try {
			String excludeList = data.getOptionValues('exclude.update')[0]
			def excludeItems = excludeList.split(',')
			excludeItems.each { item ->
				excludes[item] = item
			}
		} catch (e) {}
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
		if (excludes['meta'] == null) {
			def updated = processTemplateService.updateWorkitemTemplates(collection, tfsProject, mapping, testTypes)
		}
		//refresh.
		if (excludes['refresh'] == null) {
			def testItems = clmTestManagementService.getTestItemsViaQuery(wiQuery)
			while (true) {
				def changeList = []
				def filtered = filtered(testItems, wiFilter)
				filtered.each { workitem ->
					int id = Integer.parseInt(workitem.id.text())
					changeList.add(id)
				}
				def wiChanges = workManagementService.refreshCache(collection, tfsProject, changeList)
				def rel = testItems.@rel
				if ("${rel}" != 'next') break
					testItems = clmTestManagementService.nextPage(testItems.@href)
			}
		}
		//translate work data.
		if (excludes['data'] == null) {
			def translateMapping = processTemplateService.getTranslateMapping(collection, tfsProject, mapping, testTypes)
			def testItems = clmTestManagementService.getTestItemsViaQuery(wiQuery)
			def memberMap = memberManagementService.getProjectMembersMap(collection, tfsProject)
			while (true) {
				//TODO: ccmWorkManagementService.resetNewId()
				def changeList = []
				def idMap = [:]
				int count = 0
				def filtered = filtered(testItems, wiFilter)
				filtered.each { workitem ->
					int id = Integer.parseInt(workitem.id.text())
					// TODO: def wiChanges = ccmWorkManagementService.getWIChanges(id, tfsProject, translateMapping, memberMap)
					if (wiChanges != null) {
						idMap[count] = "${id}"
						changeList.add(wiChanges)
						count++
					}
				}
				if (changeList.size() > 0) {
					workManagementService.batchWIChanges(collection, tfsProject, changeList, idMap)
				}
				def rel = testItems.@rel
				if ("${rel}" != 'next') break
					testItems = clmTestManagementService.nextPage(testItems.@href)
			}
		}
		//		workManagementService.testBatchWICreate(collection, tfsProject)
		//apply work links
		if (excludes['links'] == null) {
			def linkMapping = processTemplateService.getLinkMapping(mapping)
			def testItems = clmTestManagementService.getTestItemsViaQuery(wiQuery)
			while (true) {
				def changeList = []
				def idMap = [:]
				int count = 0
				def filtered = filtered(testItems, wiFilter)
				filtered.each { workitem ->
					int id = Integer.parseInt(workitem.id.text())
					def wiChanges = ccmWorkManagementService.getWILinkChanges(id, tfsProject, linkMapping)
					if (wiChanges != null) {
						idMap[count] = "${id}"
						changeList.add(wiChanges)
						count++
					}
				}
				if (changeList.size() > 0) {
					workManagementService.batchWIChanges(collection, tfsProject, changeList, idMap)
				}
				def rel = testItems.@rel
				if ("${rel}" != 'next') break
					testItems = clmTestManagementService.nextPage(testItems.@href)
			}
		}

		//extract & apply attachments.
		if (excludes['attachments'] == null) {
			def linkMapping = processTemplateService.getLinkMapping(mapping)
			def workItems = clmWorkItemManagementService.getWorkItemsViaQuery(wiQuery)
			while (true) {
				def changeList = []
				def idMap = [:]
				int count = 0
				def filtered = filtered(workItems, wiFilter)
				filtered.each { workitem ->
					int id = Integer.parseInt(workitem.id.text())
					def files = attachmentsManagementService.cacheWorkItemAttachments(id)
					def wiChanges = fileManagementService.ensureAttachments(collection, tfsProject, id, files)
					if (wiChanges != null) {
						idMap[count] = "${id}"
						changeList.add(wiChanges)
						count++
					}
				}
				if (changeList.size() > 0) {
					workManagementService.batchWIChanges(collection, tfsProject, changeList, idMap)
				}
				def rel = workItems.@rel
				if ("${rel}" != 'next') break
					workItems = clmWorkItemManagementService.nextPage(workItems.@href)
			}
		}

		ccmWorkManagementService.rtcRepositoryClient.shutdownPlatform()
	}

	def filtered(def workItems, String filter) {
		if (this.filterMap[filter] != null) {
			return this.filterMap[filter].filter(workItems)
		}
		return workItems.workItem.findAll { wi ->
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
		def required = ['clm.url', 'clm.user', 'clm.password', 'clm.projectArea', 'qm.template.dir', 'tfs.url', 'tfs.user', 'tfs.token', 'tfs.project', 'test.mapping.file', 'qm.query', 'qm.filter']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}



}
