package com.zions.vsts.services.cli.action.wit

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.work.templates.ProcessTemplateService

import groovy.json.JsonBuilder
import groovy.xml.MarkupBuilder

/**
 * Class to write template data in XML form.
 * 
 * @author Astro
 *
 */
@Component
class WriteWorkItemTypes implements CliAction {
	@Autowired
	ProcessTemplateService processTemplateService;
	
	@Autowired
	public WriteWorkItemTypes() {
		this.processTemplateService = processTemplateService
	}

	public def execute(ApplicationArguments data) {
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		String project = data.getOptionValues('tfs.project')[0]
		String workItemName = data.getOptionValues('tfs.workitem.names')[0]
		def outDir = data.getOptionValues('export.dir')[0];
		def wits = processTemplateService.getWorkItemTypes(collection, project, 'layout')
		
		// TODO: Sort wits.value
		
		// see:  /zions-ext-cli/src/main/groovy/com/zions/clm/services/cli/action/wit/BuildWITStarter.groovy
		
		// MarkupBuilder is used to create XML
		def writer = new StringWriter()
		MarkupBuilder bXml = new MarkupBuilder(writer)  //TODO:  Study up on examples of MarkupBuilder
		bXml.PROCESSTEMPLATE(name:'ZionsAgile') {
			// TODO:  change wits.value to variable of sorted results.
			wits.value.each { wit -> 
				WORKITEMTYPE(name: "${wit.name}", referenceName: "${wit.referenceName}")
				
				def fields = processTemplateService.getWorkitemTemplateFields(collection, project, "${wit.referenceName}")
				
				// TODO: Sort layouts,  Basically anything that is a list/collection sort it.
				// TODO: Build XML from wits and layout
			}
		}
		
		File outFile = new File("${outDir}/processTemplate.xml")
		def o = outFile.newDataOutputStream()
		o << writer.toString()
		o.close();
		return null;
	}
	

	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.token', 'tfs.project', 'export.dir']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	


}
