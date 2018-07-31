package com.zions.vsts.services.cli.action.wit

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.work.templates.ProcessTemplateService

import groovy.json.JsonBuilder
import groovy.xml.MarkupBuilder

@Component
class ExportWIT implements CliAction {
	ProcessTemplateService processTemplateService;
	
	@Autowired
	public ExportWIT(ProcessTemplateService processTemplateService) {
		this.processTemplateService = processTemplateService
	}

	public def execute(ApplicationArguments data) {
		String collection = data.getOptionValues('tfs.collection')[0]
		String project = data.getOptionValues('tfs.project')[0]
		String workItemName = data.getOptionValues('tfs.workitem.name')[0]
		if ("${workItemName}" == 'all') {
			def wits = processTemplateService.getWorkItems(collection, project)
			wits.value.each { wit -> 
				
				def xml = processTemplateService.getWorkitemTemplateXML(collection, project, "${wit.name}")
				def outFile = data.getOptionValues('template.dir')[0];
				File oFile = new File("${outFile}/${wit.name}.xml");
				def w = oFile.newWriter();
				w << "${xml}"
				w.close();
	
			}
		} else {
			def xml = processTemplateService.getWorkitemTemplateXML(collection, project, workItemName)
			def outFile = data.getOptionValues('template.dir')[0];
			File oFile = new File("${outFile}/${workItemName}.xml");
			def w = oFile.newWriter();
			w << "${xml}"
			w.close();
		}
		return null;
	}

	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.collection', 'tfs.token', 'tfs.project', 'tfs.workitem.name','template.dir']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	


}
