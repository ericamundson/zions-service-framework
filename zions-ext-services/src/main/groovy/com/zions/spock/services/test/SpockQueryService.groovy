package com.zions.spock.services.test

import groovy.json.JsonSlurper
import org.springframework.stereotype.Component

@Component
class SpockQueryService {
	
	def loadAllReports(File topDir) {
		def reports = []
		topDir.eachFileRecurse { File file ->
			if (file.name == 'spock-report.json') {
				reports.add( file )
			}
		}
		def testCase = []
		reports.each { File report -> 
			String fText = report.text
			fText = fText.replace('loadLogFile(', '{ "loadLogFile" :')
			fText = fText.replace(')', '},')
			fText = fText.substring(0, fText.length()-3)
			fText = "[ ${fText} ]"
			def logs = new JsonSlurper().parseText(fText)
			formatToTestCaseInfo(logs, testCase)
		}
		return testCase
	}
	
	def formatToTestCaseInfo( logs, testCase ) {
		logs.each { log ->
			log.loadLogFile.each { entry -> 
				if (entry.features) {
					String sPackage = entry.'package'
					String testClass = entry.name
					def features = entry.features
					features.each { feature ->
						String title = "${sPackage}.${testClass}: ${feature.name}"
						def testCaseInfo = [title: title, stepsOut: feature.output, result: feature.result]
						testCase.add(testCaseInfo)
					}
				}
			}
		}
	}
}
