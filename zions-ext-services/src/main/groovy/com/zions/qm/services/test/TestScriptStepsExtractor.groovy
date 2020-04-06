package com.zions.qm.services.test

import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import groovy.xml.XmlUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.qm.services.test.handlers.StepsHandler
import com.zions.qm.services.test.ClmTestManagementService

@Slf4j
class TestScriptStepsExtractor {

	def steps = []

	def TestScriptStepsExtractor(def testscript, def stepsHandler, def clmAttachmentManagementService, def targetDir) {
		
		// Use Steps handler to extract the steps description and expected results
		def value = stepsHandler.buildStepData(testscript, "${testscript.webId}")
		if (value != null) {
			def parsedSteps 
			try {
				parsedSteps = new XmlSlurper().parseText(value)
			}
			catch (Exception e) {
				log.error("Error parsing steps for test script ${testscript.webId}: ${e.message}")
				return
			}
			
			// Format into a collection of Step objects
			def iStep = 0
			parsedSteps.children().each { step ->
				def description = "${step.parameterizedString[0]}"
				def expResults = "${step.parameterizedString[1]}"
				def attachments = clmAttachmentManagementService.archiveAttachments(testscript, "${step.parameterizedString[2]}", targetDir)
				this.steps.add(new StepData(description, expResults, attachments))	
				iStep++	
			}
		}
	}


	class StepData {
		String description
		String expectedResult
		String attachment
		def StepData(def desc, def expResult, def att) {
			description = desc
			expectedResult = expResult
			attachment = att
		}
	}

}


