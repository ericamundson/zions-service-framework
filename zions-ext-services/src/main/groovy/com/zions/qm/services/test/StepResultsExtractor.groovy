package com.zions.qm.services.test

import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import groovy.xml.XmlUtil
import org.springframework.beans.factory.annotation.Autowired
import com.zions.qm.services.test.handlers.StepResultsHandler
import com.zions.qm.services.test.ClmTestManagementService

class StepResultsExtractor {

	def steps = []

	def StepResultsExtractor(def result, def stepsHandler, def clmAttachmentManagementService, def targetDir) {
		
		// Use Steps handler to extract the steps description and expected results
		def value = stepsHandler.buildStepData(result, "${result.webId}")
		if (value != null) {
			def parsedSteps = new XmlSlurper().parseText(value)
			
			// Format into a collection of Step objects
			def iStep = 0
			parsedSteps.children().each { step ->
				def description = "${step.parameterizedString[0]}"
				def expResults = "${step.parameterizedString[1]}"
				def resultVal = "${step.parameterizedString[2]}"
				def endTime = "${step.parameterizedString[3]}"
				def attachments = archiveAttachments(result, "${step.parameterizedString[4]}", clmAttachmentManagementService, targetDir)
				this.steps.add(new StepData(description, expResults, resultVal, endTime, attachments))	
				iStep++	
			}
		}
	}

	def archiveAttachments (def result, def attachmentHrefs, def clmAttachmentManagementService, def targetDir)  {
		List hrefList = attachmentHrefs.split("\\|")
		if (hrefList[0] == '') return ''
		// Get any attachments for this step
		List files = clmAttachmentManagementService.cacheTestItemAttachments(hrefList)
		def attachments = ''
		files.each { file ->
			def fname = "${result.webId.text()}-${file.fileName}"
			archiveFile(fname, "$targetDir", file.file)
			attachments = attachments + "\n$targetDir\\$fname"
		}
		return attachments
	}

	def archiveFile(String fname, String dir, byte[] byteArray) {
		// Write out file
		try {
			new File("$dir/$fname").withOutputStream {
				it.write byteArray
			}
		}
		catch (e) {
			log.error("Could not save file $fname.  Error: ${e.getMessage()}")
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


