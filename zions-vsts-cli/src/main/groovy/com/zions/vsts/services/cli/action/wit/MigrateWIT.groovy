package com.zions.vsts.services.cli.action.wit

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.rest.IGenericRestClient
import com.zions.vsts.services.work.templates.ProcessTemplateService

import groovy.json.JsonBuilder
import groovy.xml.MarkupBuilder
import groovyx.net.http.RESTClient

@Component
class MigrateWIT implements CliAction {
	@Autowired
	ProcessTemplateService processTemplateService;
	
	
	@Autowired
	IGenericRestClient genericRestClient
	
	@Autowired
	public MigrateWIT() {
	}

	public def execute(ApplicationArguments data) {
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		
//		adoProperties.url = data.getOptionValues('tfs.source.url')[0]
//		adoProperties.user = data.getOptionValues('tfs.source.user')[0]
//		adoProperties.token = data.getOptionValues('tfs.source.token')[0]
//		String sourceProject = data.getOptionValues('tfs.source.project')[0]
//		String workItemNames = data.getOptionValues('tfs.workitem.names')[0]
//		RESTClient client = new RESTClient(adoProperties.url)
//		client.ignoreSSLIssues()
//		client.handler.failure = { it }
//		genericRestClient.setRestClient(client)
//		genericRestClient.setProxy()
//		genericRestClient.setCredentials(adoProperties.user, adoProperties.user)
//		def witsOut = []
//		if ("${workItemNames}" == 'all') {
//			def wits = processTemplateService.getWorkItemTypes(collection, sourceProject)
//			wits.value.each { wit -> 
//				def witChanges = processTemplateService.translateWitChanges(collection, sourceProject, "${wit.name}")
//				witsOut.add(witChanges)
//			}
//		} else {
//			String[] witNames = data.getOptionValues('tfs.workitem.names')[0].split(',')
//			witNames.each { name -> 
//				def witChanges = processTemplateService.translateWitChanges(collection, sourceProject, "${name}")
//				witsOut.add(witChanges)
//			}
//		}
//		
//		// Output stuff.
//		String targetProject = data.getOptionValues('tfs.target.project')[0]
//		adoProperties.url = data.getOptionValues('tfs.target.url')[0]
//		adoProperties.user = data.getOptionValues('tfs.target.user')[0]
//		adoProperties.token = data.getOptionValues('tfs.target.token')[0]
//		client = new RESTClient(adoProperties.url)
//		client.ignoreSSLIssues()
//		client.handler.failure = { it }
//		genericRestClient.setRestClient(client)
//		genericRestClient.setProxy()
//		genericRestClient.setCredentials(adoProperties.user, adoProperties.user)
//		
//		processTemplateService.ensureWITChanges(collection, targetProject, witsOut, true)
//		return null;
	}

	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.source.url', 'tfs.source.user', 'tfs.source.token', 'tfs.source.project','tfs.target.url', 'tfs.target.user', 'tfs.target.token', 'tfs.target.project', 'tfs.workitem.names']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	


}
