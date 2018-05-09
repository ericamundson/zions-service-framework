package com.zions.vsts.services.cli.action.wit;

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.work.templates.service.ProcessTemplateService

@Component
class ImportWIT implements CliAction {
	ProcessTemplateService processTemplateService
	Map actionsMap
	
	@Autowired
	public ImportWIT(Map actionsMap, ProcessTemplateService processTemplateService) {
		this.actionsMap = actionsMap;
		this.processTemplateService = processTemplateService
	}

	@Override
	public def execute(ApplicationArguments data) {
		String collection = data.getOptionValues('tfs.collection')[0]
		String project = data.getOptionValues('tfs.project')[0]
		String workItemName = data.getOptionValues('tfs.workitem.name')[0]
		String inFile = data.getOptionValues('in.file.name')[0]
		String body = new File(inFile).text
		def template = processTemplateService.updateWorkitemTemplate(collection, project, workItemName, body)
		if (template == null) {
			throw new Exception('Unable to update WIT');
		}
		return null;
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.collection', 'tfs.token', 'tfs.project', 'tfs.workitem.name','in.file.name']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	
}