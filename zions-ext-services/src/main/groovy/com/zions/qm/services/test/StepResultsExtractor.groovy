package com.zions.qm.services.test

import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import groovy.xml.XmlUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.qm.services.test.handlers.StepResultsHandler
import com.zions.qm.services.test.ClmTestManagementService

@Slf4j
class StepResultsExtractor {

	def steps = []

	def StepResultsExtractor(def result, def stepsHandler, def clmAttachmentManagementService, def targetDir) {
		
		// Use Steps handler to extract the steps description and expected results
		def value = stepsHandler.buildStepData(result, "${result.webId}")
		if (value != null) {
			def parsedSteps
			try {
				parsedSteps = new XmlSlurper().parseText(value)
			}
			catch (Exception e) {
				log.error("Error parsing steps for test result ${result.webId}: ${e.message}")
				return
			}
			
			// Format into a collection of Step objects
			def iStep = 0
			parsedSteps.children().each { step ->
				def description = "${step.parameterizedString[0]}"
				def expResults = "${step.parameterizedString[1]}"
				def resultVal = "${step.parameterizedString[2]}"
				def endTime = "${step.parameterizedString[3]}"
				def attachments = clmAttachmentManagementService.archiveAttachments(result, "${step.parameterizedString[4]}", targetDir)
				this.steps.add(new StepData(description, expResults, resultVal, endTime, attachments))	
				iStep++	
			}
		}
	}


	class StepData {
		String description
		String expectedResult
		String result
		String endTime
		String attachment
		def StepData(def desc, def expResult, def resultVal, def etime, def att) {
			description = desc
			expectedResult = expResult
			result = resultVal
			endTime = etime
			attachment = att
		}
	}

}


