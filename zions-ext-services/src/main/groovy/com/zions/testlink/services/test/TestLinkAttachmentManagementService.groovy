package com.zions.testlink.services.test

import br.eti.kinoshita.testlinkjavaapi.model.Attachment
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.rest.IGenericRestClient

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
class TestLinkAttachmentManagementService {

	@Autowired
	TestLinkClient testLinkClient

	@Autowired
	ICacheManagementService cacheManagementService
	
	@Autowired
	TestLinkMappingManagementService testLinkMappingManagementService

	public TestLinkAttachmentManagementService() {
	}

	public def cacheTestItemAttachments(def titem) {
		def files = []
		String type = getOutType(titem)
		String id = "${titem.webId.text()}-${type}"
		testLinkClient.
		titem.attachment.each { attachment ->
			String aurl = "${attachment.@href}"
			def result = clmTestManagementService.getContent(aurl)
			if (result.filename != null) {
				def file = cacheManagementService.saveBinaryAsAttachment(result.data, result.filename, id)
				def item = [file: file, comment: "Added attachment ${result.filename}"]
				//File cFile = saveAttachment
				files.add(item)
			}
		}

		return files
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
		Attachment[] attachments = testLinkClient.getTestCaseAttachments(testCase.id, testCase.fullExternalId)
		attachments.each { Attachment attachment ->
			def data = attachment.content
			def filename = attachment.fileName
			String id = "${testCase.id}"
			if (filename != null) {
				def file = cacheManagementService.saveBinaryAsAttachment(data, filename, id)
				def item = [file: file, comment: "Added attachment ${filename}"]
				//File cFile = saveAttachment
				files.add(item)
			}
		}
//		def ts = getTestScript(testCase)
//		if (ts != null)	{
//			def stepFiles = handleTestSteps(ts, id)
//			stepFiles.each { file ->
//				files.add(file)
//			}
//		}
		return files
	}

	private def handleTestSteps(ts, id) {
		def files = []
		int sCount = 2
		ts.steps.step.each { step ->
			String comment = "[TestStep=${sCount}]:"
			step.attachment.each { attachment ->
				String aurl = "${attachment.@href}"
				def result = clmTestManagementService.getContent(aurl)
				if (result.filename != null) {
					def file = cacheManagementService.saveBinaryAsAttachment(result.data, "${result.filename}", id)
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
	
	private def getOutType(qmItemData) {
		String type = "${qmItemData.name()}"
		def maps = testMappingManagementService.mappingData.findAll { amap ->
			"${amap.source}" == "${type}"
		}
		if (maps.size() > 0) {
			return maps[0].target
		}
		return maps
	}

}
