package com.zions.testlink.services.test

import br.eti.kinoshita.testlinkjavaapi.model.Attachment
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.rest.IGenericRestClient

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import br.eti.kinoshita.testlinkjavaapi.TestLinkAPIException

/**
 * Manage caching attachments and returning data elements required to associate attachment to ADO element.
 * 
 * <p>Design</p>
 * <img src="TestLinkAttachmentManagementService.svg"/>
 * 
 * @author z091182
 *
 * @startuml
 * class TestLinkAttachmentManagementService [[java:com.zions.testlink.services.test.TestLinkAttachmentManagementService]] {
 * 	~String cacheLocation
 * 	+TestLinkAttachmentManagementService()
 * 	+def cacheTestCaseAttachments(def testCase)
 * }
 * TestLinkAttachmentManagementService --> TestLinkClient: @Autowired testlinkClient
 * TestLinkAttachmentManagementService --> ICacheManagementService: @Autowired cacheManagementService
 * TestLinkAttachmentManagementService --> TestLinkMappingManagementService: @Autowired testLinkMappingManagementService
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

	/**
	 * Cache all test case attachments including script and steps.
	 * 
	 * @param testCase 
	 * @param id
	 * @return
	 */
	public def cacheTestCaseAttachments(def testCase) {
		def files = []
		Attachment[] attachments = []
		try {
			attachments = testLinkClient.getTestCaseAttachments(testCase.id, null)
		}
		catch (TestLinkAPIException e) {
			log.error("Attempt to retrieve attachments failed for test case <${testCase.name}(${testCase.id})>.  Error: $e")
		}
		attachments.each { Attachment attachment ->
			def data = attachment.content
			byte[] dstr = Base64.decoder.decode(data)
			//ByteArrayInputStream bdata = new ByteArrayInputStream(dstr)
			def filename = attachment.fileName
			String id = "${testCase.id}"
			if (filename != null) {
				//def file = cacheManagementService.saveBinaryAsAttachment(bdata, filename, id)
				def item = [file: dstr, fileName: filename, comment: "Added attachment ${filename}"]
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


}
