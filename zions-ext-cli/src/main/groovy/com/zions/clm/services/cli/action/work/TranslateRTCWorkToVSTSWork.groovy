package com.zions.clm.services.cli.action.work

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.clm.services.ccm.workitem.CcmWorkManagementService
import com.zions.clm.services.ccm.workitem.metadata.CcmWIMetadataManagementService
import com.zions.clm.services.rtc.project.workitems.ClmWorkItemManagementService
import com.zions.clm.services.rtc.project.workitems.RtcWIMetadataManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.work.WorkManagementService
import com.zions.vsts.services.work.templates.ProcessTemplateService
import groovy.json.JsonBuilder

@Component
class TranslateRTCWorkToVSTSWork implements CliAction {
	@Autowired
	CcmWIMetadataManagementService ccmWIMetadataManagementService;
	@Autowired
	ProcessTemplateService processTemplateService;
	@Autowired
	WorkManagementService workManagementService;
	@Autowired
	ClmWorkItemManagementService clmWorkItemManagementService
	@Autowired
	CcmWorkManagementService ccmWorkManagementService

	public TranslateRTCWorkToVSTSWork() {
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
		String templateDir = data.getOptionValues('ccm.template.dir')[0]
		String mappingFile = data.getOptionValues('wit.mapping.file')[0]
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		String tfsProject = data.getOptionValues('tfs.project')[0]
		File mFile = new File(mappingFile)
		
		def mapping = new XmlSlurper().parseText(mFile.text)
		def ccmWits = loadCCMWITs(templateDir)
		//Update TFS wit definitions.
		if (!excludes['meta'] == null) {
			def updated = processTemplateService.updateWorkitemTemplates(collection, tfsProject, mapping, ccmWits)
		}
		//translate work data.
		def translateMapping = processTemplateService.getTranslateMapping(collection, project, mapping, ccmWits)
		def workItems = clmWorkItemManagementService.getWorkItemsForProject(project)
		while (true) {
			def changeList = []
			workItems.workItem.each { workitem ->
				int id = Integer.parseInt(workitem.id.text())
				def wiChanges = ccmWorkManagementService.getWIChanges(id, project, translateMapping)
				changeList.add(wiChanges)
			}
			workManagementService.batchWIChanges(collection, project, changeList)
			def rel = workItems.@rel
			if ("${rel}" != 'next') break
			workItems = clmWorkItemManagementService.nextPage(workItems.@href)
		}
		workManagementService.testBatchWICreate(collection, tfsProject)
		//apply work links
		//extract attachments.
		//apply attachments.
		
		
	}
	
	def loadCCMWITs(def ccmTemplateDir) {
		def wits = []
		File tDir = new File(ccmTemplateDir)
		if (tDir.exists() || tDir.isDirectory()) {
			tDir.eachFile { file ->
				def witData = new XmlSlurper().parse(file)
				wits.add(witData)
			}
		}
		return wits
	}

	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['clm.url', 'clm.user', 'clm.password', 'clm.projectArea', 'ccm.template.dir', 'tfs.url', 'tfs.user', 'tfs.token', 'tfs.project', 'wit.mapping.file' ]
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	


}
