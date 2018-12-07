package com.zions.qm.services.test

import com.zions.common.services.rest.IGenericRestClient
import com.zions.ext.services.cache.CacheManagementService
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
	 * @param testCase - E.G. [[filename: 'dummy.jpg', url: 'https://someurl'], [filename: 'dummy2.jpg', url: 'https://someurl2']]
	 * @param id
	 * @return
	 */
	public def cacheAttachments(def testCase) {
		def files = []
		String id = "${testCase.webId.text()}-Test Case"
		
		def ts = getTestScript(testCase)
		if (ts != null)	{
			def stepFiles = handleTestSteps(ts, id)
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
				//File cFile = saveAttachment
			}
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
