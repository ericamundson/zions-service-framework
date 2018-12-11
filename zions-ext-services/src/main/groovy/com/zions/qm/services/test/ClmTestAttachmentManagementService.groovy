package com.zions.qm.services.test

import com.zions.common.services.rest.IGenericRestClient
import com.zions.ext.services.cache.CacheManagementService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Manage caching attachments and returning data elements required to associate attachment to ADO element.
 * 
 * @author z091182
 *
 * @startuml
 * class ClmTestAttachmentManagementService [[java:com.zions.qm.services.test.ClmTestAttachmentManagementService]] {
 * 	~String cacheLocation
 * 	+ClmTestAttachmentManagementService()
 * 	+def cacheAttachments(def testCase)
 * 	-def handleTestSteps()
 * 	-def saveAttachment(def result, String id)
 * 	-def getTestScript(def itemData)
 * }
 * @enduml
 */
@Component
@Slf4j
class ClmTestAttachmentManagementService {

	@Autowired
	IGenericRestClient qmGenericRestClient

	@Autowired
	ClmTestManagementService clmTestManagementService

	@Autowired
	CacheManagementService cacheManagementService

	public ClmTestAttachmentManagementService() {
	}

	/**
	 * Cache all test case attachments including script and steps.
	 * 
	 * @param testCase 
	 * @param id
	 * @return
	 */
	public def cacheTestCaseAttachments(def testCase) {
		def files = []
		String id = "${testCase.webId.text()}-Test Case"
		testCase.attachment.each { attachment ->
			String aurl = "${attachment.@href}"
			def result = clmTestManagementService.getContent(aurl)
			String cd = "${result.headers.'Content-Disposition'}"
			
			String[] sItem = cd.split('=')
			String filename = null
			if (sItem.size() == 2) {
				filename = sItem[1]
				filename = filename.replace('"', '')
			}
			if (filename != null) {
				def file = cacheManagementService.saveBinaryAsAttachment(result.data, "${filename}", id)
				def item = [file: file, comment: "Added attachment ${filename}"]
				//File cFile = saveAttachment
				files.add(item)
			}
		}
		def ts = getTestScript(testCase)
		if (ts != null)	{
			def stepFiles = handleTestSteps(ts, id)
			stepFiles.each { file ->
				files.add(file)
			}
		}
		return files
	}

	private def handleTestSteps(ts, id) {
		def files = []
		int sCount = 2
		ts.steps.step.each { step ->
			String comment = "[TestStep:${sCount}]:"
			step.attachment.each { attachment ->
				String aurl = "${attachment.@href}"
				def result = clmTestManagementService.getContent(aurl)
				String cd = "${result.headers.'Content-Disposition'}"
				
				String[] sItem = cd.split('=')
				String filename = null
				if (sItem.size() == 2) {
					filename = sItem[1]
					filename = filename.replace('"', '')
				}
				if (filename != null) {
					def file = cacheManagementService.saveBinaryAsAttachment(result.data, "${filename}", id)
					def item = [file: file, comment: comment]
					//File cFile = saveAttachment
					files.add(item)
				}
			}
			sCount++
		}
		return files
	}



	private def getTestScript(def itemData) {
		def tss = itemData.testscript
		if (tss.size() > 0) {
			String href = "${tss[0].@href}"
			def ts = clmTestManagementService.getTestItem(href)
			return ts
		}
		return null
	}
}
