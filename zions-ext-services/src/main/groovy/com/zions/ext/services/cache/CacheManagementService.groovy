package com.zions.ext.services.cache

import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class CacheManagementService {
	
	@Autowired
	@Value('${cache.location}')
	String cacheLocation

	public CacheManagementService(String cacheLocation) {
		this.cacheLocation = cacheLocation
	}
	
	public CacheManagementService() {}
	
	def saveBinaryAsAttachment(ByteArrayInputStream result, String name, String id) {
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
			File save = new File("${this.cacheLocation}${File.separator}${id}${File.separator}attachments${File.separator}${name}");
			if (!save.exists()) {
				DataOutputStream os = save.newDataOutputStream()
				os << result.bytes
				os.close()
			}
			return save
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
		} catch (IOException e) {
			// TODO Auto-generated catch block
		}
	
	}

	/**
	 * Check cache for work item state.
	 *
	 * @param id
	 * @return
	 */
	def getCacheWI(id) {
		File cacheData = new File("${this.cacheLocation}${File.separator}${id}${File.separator}wiData.json");
		if (cacheData.exists()) {
			JsonSlurper s = new JsonSlurper()
			return s.parse(cacheData)
		}
		return null

	}
	
	def getResultData(String id) {
		File cacheData = new File("${this.cacheLocation}${File.separator}${id}${File.separator}resultData.json");
		if (cacheData.exists()) {
			JsonSlurper s = new JsonSlurper()
			return s.parse(cacheData)
		}
		return null

	}

}
