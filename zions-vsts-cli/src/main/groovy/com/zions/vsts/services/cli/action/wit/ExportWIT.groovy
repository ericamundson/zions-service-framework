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
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		String project = data.getOptionValues('tfs.project')[0]
		String workItemName = data.getOptionValues('tfs.workitem.names')[0]
		def outFile = data.getOptionValues('export.dir')[0];
		if ("${workItemName}" == 'all') {
			def wits = processTemplateService.getWorkItemTypes(collection, project)
			wits.value.each { wit -> 
				def witChanges = processTemplateService.translateWitChanges(collection, project, "${wit.name}")
				File oFile = new File("${outFile}/${wit.name}.json");
				def w = oFile.newWriter();
				w << new JsonBuilder(witChanges).toPrettyString()
				w.close();
	
			}
		} else {
			String[] witNames = data.getOptionValues('tfs.workitem.names')[0].split(',')
			witNames.each { name -> 
				def witChanges = processTemplateService.translateWitChanges(collection, project, "${name}")
				File oFile = new File("${outFile}/${name}.json");
				def w = oFile.newWriter();
				w << new JsonBuilder(witChanges).toPrettyString()
				w.close();
			}
		}
		return null;
	}
	

	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.token', 'tfs.project', 'tfs.workitem.names','export.dir']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	


}
