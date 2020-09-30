package com.zions.vsts.services.action.test

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import com.zions.common.services.cli.action.CliAction
import com.zions.vsts.services.test.TestManagementService
import com.zions.vsts.services.work.templates.ProcessTemplateService

import groovy.json.JsonBuilder
import groovy.xml.MarkupBuilder

@Component
class CopyTestPlans implements CliAction {
	@Autowired
	TestManagementService testManagementService;
	
	//@Value('${tfs.types:}')
	@Value('${tfs.testplanid}')
	String[] testplanIds

	@Value('${tfs.testplanDestName}')
	String testplanDestName
	
	@Value('${tfs.testplanDestProject}')
	String testplanDestProject
	
	@Value('${tfs.testplanSrcProject}')
	String testplanSrcProject
	
	//use for both Area Path/Iteration path
	@Value('${tfs.testplanPath}')
	String testplanPath
	

	//@Value('${tfs.types:}')

	
	public CopyTestPlans() {
	}

	 	public def execute(ApplicationArguments data) {
        //call method on test management service called "CopyTestPlan
	    // pass source test plan id, source project, destination project, test plan name, and test area/iteration
	    //Implement all in the testmanagement service - in the copyTestPlan
			 
        ///* Write code call cloneTestPlan from TestManagementService
			 //@value for collection @value for all input parameters
			 //add autowire for testmanagement service
			 //make sure getting all params to test management service
			 //make sure body is building properly with all values
			 //consider using one value for testplanName - just give it test plan id/ name of the destination project.
			 //add testplanId.each then pass it to the testplan clone method
		
		def testplanClone = "https://dev.azure.com/zionseto/${testplanSrcProject}/_apis/test/Plans/${testplanId}/cloneoperation?api-version=5.0-preview.2"
		def copyTestPlan = [method:'POST', uri: "/_apis/test/Plans/${testplanId}/cloneoperation?api-version=5.0-preview.2&bypassRules=true", headers: ['Content-Type': 'application/json-patch+json'], body: []]
		/*
		 * BODY SHOULD LOOK LIKE THIS
				 * {
		  "destinationTestPlan": {
		    "name": "SOPP int. – Duplicate Payments",
		    "Project": {
		      "Name": "Sandbox"
		    }
		  },
		  "options": {
		    "copyAncestorHierarchy": true,
		    "copyAllSuites": true,
		    "overrideParameters": {
		      "System.AreaPath": "Sandbox",
		      "System.IterationPath": "Sandbox"
		    }
		  },
		  "suiteIds": [
		    2
		  ]
		}

		 */
		
		
		def idData = [ op: 'add', path: "/fields/$adoFieldName", value: "$value"]
		copyTestPlan.body.add(idData)
		

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
