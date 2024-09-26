package com.zions.common.services.spock.test

import groovy.json.JsonSlurper
import groovy.xml.XmlSlurper
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
			String durationStr = "${feature.@time}"
			int durI = 0
			if (durationStr && durationStr.contains(' seconds')) {
				int l = ' seconds'.length()
				durationStr = durationStr.substring(0, durationStr.length() - l)
				double duration = Double.parseDouble(durationStr)
				durI = duration * 1000
			}
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
			def testCaseInfo = [title: title, blocks: bs, duration: durI, result: result]
			testCase.add(testCaseInfo)
		}
	
	}
}
