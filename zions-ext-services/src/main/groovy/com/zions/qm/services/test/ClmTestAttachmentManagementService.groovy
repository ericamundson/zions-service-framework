package com.zions.qm.services.test

import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.rest.IGenericRestClient

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Manage caching attachments and returning data elements required to associate attachment to ADO element.
 * 
 * <p><b>Design:</b></p>
 * <img src="ClmTestAttachmentManagementService.png"/>
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
	ICacheManagementService cacheManagementService
	
	@Autowired
	TestMappingManagementService testMappingManagementService

	public ClmTestAttachmentManagementService() {
	}

	public def cacheTestItemAttachments(def titem) {
		def files = []
		String type = getOutType(titem)
		String id = "${titem.webId.text()}-${type}"
		titem.attachment.each { attachment ->
			String aurl = "${attachment.@href}"
			def result = clmTestManagementService.getContent(aurl)
			if (result.filename != null) {
				String fName = cleanTextContent(result.filename)
				//def file = cacheManagementService.saveBinaryAsAttachment(result.data, fName, id)
				ByteArrayInputStream s = result.data
				def file = s.bytes
				def item = [file: file, fileName: fName, comment: "Added attachment ${fName}"]
				//File cFile = saveAttachment
				files.add(item)
			}
		}

		return files
	}

	public def cacheTestItemAttachments(List hrefs) {
		def files = []
		hrefs.each { aurl ->
			def result = clmTestManagementService.getContent(aurl)
			if (result.filename != null) {
				String fName = cleanTextContent(result.filename)
				//def file = cacheManagementService.saveBinaryAsAttachment(result.data, fName, id)
				ByteArrayInputStream s = result.data
				if (s != null) {
					def file = s.bytes
					def item = [file: file, fileName: fName, comment: "Added attachment ${fName}"]
					//File cFile = saveAttachment
					files.add(item)
				}
			}
		}

		return files
	}

		
	public def cacheTestItemAttachmentsAsBinary(def titem) {
		def binaries = []
//		String type = getOutType(titem)
//		String id = "${titem.webId.text()}-${type}"
		titem.attachment.each { attachment ->
			String aurl = "${attachment.@href}"
			def result = clmTestManagementService.getContent(aurl)
			if (result.filename != null && result.data) {
				String fName = cleanTextContent(result.filename)
				def item = [data: result.data, filename: fName, comment: "Added attachment ${fName}"]
				//File cFile = saveAttachment
				binaries.add(item)
			}
		}

		return binaries
	}
	
	public def cacheStepAttachmentsAsBinary(def titem) {
		def binaries = []
//		String type = getOutType(titem)
//		String id = "${titem.webId.text()}-${type}"
		titem.stepAttachment.each { attachment ->
			String aurl = "${attachment.@href}"
			def result = clmTestManagementService.getContent(aurl)
			if (result.filename != null && result.data) {
				String fName = cleanTextContent(result.filename)
				def item = [data: result.data, filename: fName, comment: "Added attachment ${fName}"]
				//File cFile = saveAttachment
				binaries.add(item)
			}
		}

		return binaries
	}
	
	private static String cleanTextContent(String text)
	{
		if (text.lastIndexOf('\\') > -1) {
			text = text.substring(text.lastIndexOf('\\')+1)
		}
		// strips off all non-ASCII characters
		text = text.replaceAll("[^\\x00-\\x7F]", "");
 
		// erases all the ASCII control characters
		text = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
		 
		// removes non-printable characters from Unicode
		text = text.replaceAll("\\p{C}", "");
 
		return text.trim();
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
			if (result.filename != null) {
				String fName = cleanTextContent(result.filename)
				//def file = cacheManagementService.saveBinaryAsAttachment(result.data, fName, id)
				ByteArrayInputStream s = result.data
				byte[] file = s.bytes
				def item = [file: file, fileName: fName, comment: "Added attachment ${fName}"]
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
			String comment = "[TestStep=${sCount}]:"
			step.attachment.each { attachment ->
				String aurl = "${attachment.@href}"
				def result = clmTestManagementService.getContent(aurl)
				if (result.filename != null) {
					String fName = ClmTestAttachmentManagementService.cleanTextContent(result.filename)
					//def file = cacheManagementService.saveBinaryAsAttachment(result.data, fName, id)
					ByteArrayInputStream s = result.data
					byte[] file = s.bytes
					def item = [file: file, fileName: fName, comment: "Added attachment ${fName}"]
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
	
	def archiveAttachments (def testitem, def attachmentHrefs, def targetDir)  {
		List hrefList = attachmentHrefs.split("\\|")
		if (hrefList[0] == '') return ''
		// Get any attachments for this step
		List files = cacheTestItemAttachments(hrefList)
		def attachments = ''
		files.each { file ->
			def fname = "${testitem.webId.text()}-${file.fileName}"
			if (fname.length() > 60) fname = fname.substring(0,59)  //truncate long filenames
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

}
