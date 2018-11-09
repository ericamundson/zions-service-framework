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
		String workItemName = data.getOptionValues('tfs.workitem.name')[0]
		if ("${workItemName}" == 'all') {
			def wits = processTemplateService.getWorkItemTypes(collection, project)
			wits.value.each { wit -> 
				
				def witData = processTemplateService.getWorkitemTemplateFields(collection, project, "${wit.name}")
				def writer = new StringWriter()
				MarkupBuilder bXml = new MarkupBuilder(writer)
				bXml.'witd:WITD'(application:'Work item type editor',
					version: '1.0',
					'xmlns:witd': 'http://schemas.microsoft.com/VisualStudio/2008/workitemtracking/typedef') {
					WORKITEMTYPE(name: "${wit.name}") {
						DESCRIPTION("general work item starter")
						FIELDS {
							witData.value.each { field ->
								def fieldDetails = processTemplateService.getField(collection, project, field.referenceName)
								if (fieldDetails != null) {
									FIELD(name: "${field.name}", refname: "${field.referenceName}", type: "${fieldDetails.type}".trim(), dimension: 'reportable') {
										HELPTEXT "${field.helpText}"
										if (field.allowedValues.size() > 0) {
											ALLOWEDVALUES(expanditems: true) {
												field.allowedValues.each { value ->
													LISTITEM(value: "${value}")
												}
											}
										}
									}
								} else {
									FIELD(name: "${field.name}", refname: "${field.referenceName}", type: "${field.referenceName}", dimension: 'reportable') {
										HELPTEXT "${field.helpText}"
										if (field.allowedValues.size() > 0) {
											ALLOWEDVALUES(expanditems: true) {
												field.allowedValues.each { value ->
													LISTITEM(value: "${value}")
												}
											}
										}
									}
								}
							}
					
						}
					}
				}
				def outFile = data.getOptionValues('template.dir')[0];
				File oFile = new File("${outFile}/${wit.name}.xml");
				def w = oFile.newWriter();
				w << writer.toString()
				w.close();
	
			}
		} else {
			def xml = processTemplateService.getWorkitemTemplate(collection, project, workItemName)
			def outFile = data.getOptionValues('template.dir')[0];
			File oFile = new File("${outFile}/${workItemName}.xml");
			def w = oFile.newWriter();
			w << "${xml}"
			w.close();
		}
		return null;
	}

	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.token', 'tfs.project', 'tfs.workitem.name','template.dir']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	


}
