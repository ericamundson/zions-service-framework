package com.zions.clm.services.cli.action.wit

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.clm.services.ccm.workitem.metadata.CcmWIMetadataManagementService
import com.zions.clm.services.rtc.project.workitems.RtcWIMetadataManagementService
import com.zions.common.services.cli.action.CliAction

import groovy.json.JsonBuilder

@Component
class ExtractCcmWIMetadata implements CliAction {
	@Autowired
	CcmWIMetadataManagementService ccmWIMetadataManagementService;
	
	public ExtractCcmWIMetadata() {
	}

	public def execute(ApplicationArguments data) {
		String project = data.getOptionValues('clm.projectArea')[0]
		String templateDir = data.getOptionValues('template.dir')[0]
		def metadata = ccmWIMetadataManagementService.extractWorkitemMetadata(project, templateDir)
		ccmWIMetadataManagementService.rtcRepositoryClient.shutdownPlatform();
		return null;
	}

	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['clm.url', 'clm.user', 'clm.password', 'clm.projectArea', 'template.dir' ]
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	


}
