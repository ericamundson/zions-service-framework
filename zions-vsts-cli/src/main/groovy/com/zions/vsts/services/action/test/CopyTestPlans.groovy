package com.zions.vsts.services.action.test

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.vsts.services.work.WorkManagementService
import com.zions.common.services.excel.ExcelManagementService
import com.zions.common.services.cli.action.CliAction
import groovy.util.logging.Slf4j
import com.zions.vsts.services.test.TestManagementService
import com.zions.vsts.services.work.templates.ProcessTemplateService

import groovy.json.JsonBuilder
import groovy.xml.MarkupBuilder

@Component
@Slf4j
class CopyTestPlans implements CliAction {
	@Autowired
	TestManagementService testManagementService;
	
	//@Value('${tfs.types:}')
	@Value('${tfs.testplanIds}')
	Integer[] testplanIds

	@Value('${tfs.destPlanName}')
	String destPlanName
	
	@Value('${tfs.destProjectName}')
	String destProjectName
	
	@Value('${tfs.srcProjectName}')
	String srcProjectName
	
	@Value('${tfs.collection:}')
	String collection

	
	
	//@Value('${tfs.types:}')

	
	public CopyTestPlans() {
	}

	 public def execute(ApplicationArguments data) {
        //call method on test management service called "cloneTestPlan
        //values defined in runtime arguments for delivery to cloneTestPlan in the TestManagementService
		if (testplanIds) {
			testplanIds.each { Id ->
				//use testplan management service to get test plan name
				def plan = testManagementService.getPlan(collection, srcProjectName, Id)
				if (plan) { 
					def clonePlan = testManagementService.cloneTestPlan(collection, plan.name, Id, srcProjectName, destProjectName)
					return logResult('Update Succeeded')
				}
			}	
		}
	
	  }

	public Object validate(ApplicationArguments args) throws Exception {
		//def required = ['tfs.url', 'tfs.user', 'tfs.token', 'tfs.project', 'out.file']
		def required = ['tfs.url', 'tfs.srcProjectName', 'tfs.destPlanName', 'tfs.collection', 'tfs.testplanIds', 'tfs.destProjectName']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}
	
	private def logResult(def msg) {
		log.info(msg)
		return msg
	}

}
