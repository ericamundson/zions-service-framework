package com.zions.vsts.services.action.test

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.test.TestManagementService
import com.zions.vsts.services.work.templates.ProcessTemplateService

import groovy.json.JsonBuilder
import groovy.xml.MarkupBuilder

@Component
class ListTestRuns implements CliAction {
	@Autowired
	TestManagementService testManagementService;
	
	public ListTestRuns() {
	}

	public def execute(ApplicationArguments data) {
		String project = data.getOptionValues('tfs.project')[0]
		String outFile = data.getOptionValues('out.file')[0]
		def testRuns = testManagementService.getTestRuns(project)
		File f = new File(outFile)
		def w = f.newOutputStream()
		w << new JsonBuilder( testRuns ).toString()
		w.close()
		return null;
	}

	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.url', 'tfs.user', 'tfs.token', 'tfs.project', 'out.file']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	


}
