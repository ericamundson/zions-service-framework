package com.zions.spock.services.test

import groovy.json.JsonSlurper
import org.springframework.stereotype.Component

@Component
class SpockQueryService {
	
	def loadAllReports(File topDir) {
		def reports = []
		topDir.eachFileRecurse { File file ->
			if (file.name.endsWith('.spock.xml')) {
				reports.add( file )
			}
		}
		def testCase = []
		reports.each { File report -> 
			def testClassXml = new XmlSlurper().parse(report)
			formatToTestCaseInfo(testClassXml, testCase)
		}
		return testCase
	}
	
	def formatToTestCaseInfo( testClassXml, testCase ) {
		String className = "${testClassXml.@name}"
		testClassXml.feature.each { feature -> 
			String title = "${className}: ${feature.@name}"
			String result = "${feature.@result}" 
			def bs = []
			feature.block.each { block ->
				def b = [kind: "${block.@kind}", text: "${block.text.text()}", code: []]
				block.code.each { code ->
					String t = "${code.text()}"
					t = t.replace('\t', '  ')
					if (t.startsWith('   ')) {
						t = t.substring(4)
					}
					b.code.push(t)
				}
				bs.push(b)
			}
			def testCaseInfo = [title: title, blocks: bs, result: result]
			testCase.add(testCaseInfo)
		}
	
	}
}
