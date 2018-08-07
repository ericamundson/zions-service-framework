package com.zions.clm.services.cli.action.work

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.clm.services.ccm.workitem.metadata.CcmWIMetadataManagementService
import com.zions.clm.services.rtc.project.workitems.RtcWIMetadataManagementService
import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.work.templates.ProcessTemplateService
import groovy.json.JsonBuilder

@Component
class TranslateRTCWorkToVSTSWork implements CliAction {
	@Autowired
	CcmWIMetadataManagementService ccmWIMetadataManagementService;
	@Autowired
	ProcessTemplateService processTemplateService;

	public TranslateRTCWorkToVSTSWork() {
	}

	public def execute(ApplicationArguments data) {
		String project = data.getOptionValues('clm.projectArea')[0]
		String templateDir = data.getOptionValues('ccm.template.dir')[0]
		String mappingFile = data.getOptionValues('wit.mapping.file')[0]
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		String tfsProject = data.getOptionValues('tfs.project')[0]

		
		def mapping = new XmlSlurper().parseText(mappingFile.text)
		
		def ccmWits = loadCCMWITs(templateDir)
		//Update TFS wit definitions.
		def updated = processTemplateService.updateWorkitemTemplates(collection, tfsProject, mapping, ccmWits)
		
		//translate work data.
		//apply work links
		//extract attachments.
		//apply attachments.
		
		
	}
	
	def loadCCMWITs(def ccmTemplateDir) {
		
	}

	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['clm.url', 'clm.user', 'clm.password', 'clm.projectArea', 'ccm.template.dir', 'tfs.template.dir', 'tfs.url', 'tfs.user', 'tfs.project', 'wit.mapping.file' ]
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	


}
