package com.zions.qm.services.cli.action.metadata

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.common.services.cli.action.CliAction
import com.zions.qm.services.metadata.QmMetadataManagementService

import groovy.json.JsonBuilder

@Component
class ExtractQmMetadata implements CliAction {
	@Autowired
	QmMetadataManagementService qmMetadataManagementService;
	
	public ExtractQmMetadata() {
	}

	public def execute(ApplicationArguments data) {
		String project = data.getOptionValues('qm.projectArea')[0]
		String templateDir = data.getOptionValues('qm.template.dir')[0]
		File tDir = new File(templateDir)
		if (tDir.exists()) {
			def metadata = qmMetadataManagementService.extractQmMetadata(project, tDir)
		}
		return null;
	}

	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['clm.url', 'clm.user', 'clm.password', 'qm.projectArea', 'qm.template.dir' ]
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	


}
