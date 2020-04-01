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
			def parsedSteps = new XmlSlurper().parseText(value)
			
			// Format into a collection of Step objects
			def iStep = 0
			parsedSteps.children().each { step ->
				def description = "${step.parameterizedString[0]}"
				def expResults = "${step.parameterizedString[1]}"
				def attachments = archiveAttachments(testscript, "${step.parameterizedString[2]}", clmAttachmentManagementService, targetDir)
				this.steps.add(new StepData(description, expResults, attachments))	
				iStep++	
			}
		}
	}

	def archiveAttachments (def testscript, def attachmentHrefs, def clmAttachmentManagementService, def targetDir)  {
		List hrefList = attachmentHrefs.split("\\|")
		if (hrefList[0] == '') return ''
		// Get any attachments for this step
		List files = clmAttachmentManagementService.cacheTestItemAttachments(hrefList)
		def attachments = ''
		files.each { file ->
			def fname = "${testscript.webId.text()}-${file.fileName}"
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
		String attachment
		def StepData(def desc, def expResult, def att) {
			description = desc
			expectedResult = expResult
			attachment = att
		}
	}

}


