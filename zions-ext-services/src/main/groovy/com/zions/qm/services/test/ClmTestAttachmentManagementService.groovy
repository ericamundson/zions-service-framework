package com.zions.qm.services.test

import com.zions.common.services.rest.IGenericRestClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class ClmTestAttachmentManagementService {
	
	@Autowired
	IGenericRestClient qmGenericRestClient
	
	@Autowired
	ClmTestManagementService clmTestManagementService
	
	@Autowired
	@Value('${cache.location}')
	String cacheLocation
	
	public ClmTestAttachmentManagementService() {
	}
	
	/**
	 * @param testCase - E.G. [[filename: 'dummy.jpg', url: 'https://someurl'], [filename: 'dummy2.jpg', url: 'https://someurl2']]
	 * @param id
	 * @return
	 */
	public def cacheAttachments(def testCase) {
		def files = []
		String id = "${testCase.webId.text()}-Test Case"
		
		def ts = getTestScript(testCase)
	}
	
	def getAttachmentInfo(String url) {
		
	}
	
	def getAttachmentData(String url) {
		
	}
	
	def saveAttachment(def result, String id) {
		try {
			File cacheDir = new File(this.cacheLocation)
			if (!cacheDir.exists()) {
				cacheDir.mkdir();
			}
			
			File wiDir = new File("${this.cacheLocation}${File.separator}${id}")
			if (!wiDir.exists()) {
				wiDir.mkdir()
			}
			File attachmentDir = new File("${this.cacheLocation}${File.separator}${id}${File.separator}attachments")
			if (!attachmentDir.exists()) {
				attachmentDir.mkdir()
			}
			File save = new File("${this.cacheLocation}${File.separator}${id}${File.separator}attachments${File.separator}${attachment.getName()}");
			
			OutputStream out = save.newDataOutputStream()
			return save
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
		} catch (IOException e) {
			// TODO Auto-generated catch block
		}
	
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
