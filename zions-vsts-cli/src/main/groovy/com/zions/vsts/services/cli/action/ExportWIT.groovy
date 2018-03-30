package com.zions.vsts.services.cli.action

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.vsts.services.work.templates.service.ProcessTemplateService
import groovy.json.JsonBuilder

@Component
class ExportWIT implements CliAction {
	static public String EXPORTWIT = 'exportwit'
	ProcessTemplateService processTemplateService;
	Map actionsMap;
	
	@Autowired
	public ExportWIT(Map actionsMap, ProcessTemplateService processTemplateService) {
		this.actionsMap = actionsMap;
		this.processTemplateService = processTemplateService
		actionsMap.put('exportwit',this);
	}

	public def execute(ApplicationArguments data) {
		String collection = data.getOptionValues('tfs.collection')[0]
		String project = data.getOptionValues('tfs.project')[0]
		String workItemName = data.getOptionValues('tfs.workitem.name')[0]
		def template = processTemplateService.getWorkitemTemplate(collection, project, workItemName)
		def outFile = data.getOptionValues('out.file.name')[0];
		File oFile = new File(outFile);
		def w = oFile.newWriter();
		def json = new JsonBuilder(template)
		w << json.toPrettyString()
		w.close();
		return null;
	}

	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.collection', 'tfs.token', 'tfs.project', 'tfs.workitem.name','out.file.name']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	


}
