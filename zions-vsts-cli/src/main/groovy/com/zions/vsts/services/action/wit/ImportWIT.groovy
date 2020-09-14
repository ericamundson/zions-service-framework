package com.zions.vsts.services.action.wit;

import java.util.Map

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.common.services.logging.FlowInterceptor
import com.zions.vsts.services.work.templates.ProcessTemplateService
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

@Component
@Slf4j
class ImportWIT implements CliAction {
	@Autowired
	ProcessTemplateService processTemplateService
	
	@Autowired
	public ImportWIT() {
	}

	@Override
	public def execute(ApplicationArguments data) {
		String collection = ""
		try {
			collection = data.getOptionValues('tfs.collection')[0]
		} catch (e) {}
		String project = data.getOptionValues('tfs.project')[0]
		String exportDirName = data.getOptionValues('export.dir')[0]
		File exportDir = new File(exportDirName) 
		def wits = []
		exportDir.eachFile { file ->
			if (file.name.endsWith('.json')) {
				def witChanges = new JsonSlurper().parse(file)
				wits.add(witChanges)
			}
		}

		log.info('Processing ${wits.size()} WIT import files...')
		processTemplateService.ensureWITChanges(collection, project, wits, true)

		//this.flowLogging([processTemplateService], true, true) {  // Setup flow logging on any processTemplateService call
		//	processTemplateService.ensureWITChanges(collection, project, wits, true)
		//}
		log.info('WIT Import Completed!')		

		return null;
	}

	@Override
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['tfs.project', 'export.dir']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	
}