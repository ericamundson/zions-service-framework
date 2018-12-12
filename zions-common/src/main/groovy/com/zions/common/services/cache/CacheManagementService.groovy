package com.zions.common.services.cache

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class CacheManagementService implements ICacheManagementService {
	
	public static String WI_DATA = 'wiData'
	public static String RESULT_DATA = 'resultData'
	public static String RUN_DATA = 'runData'
	
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

	
	public def saveToCache(def data, String id, String type) {
		File cacheDir = new File(this.cacheLocation)
		if (!cacheDir.exists()) {
			cacheDir.mkdir();
		}
		File wiDir = new File("${this.cacheLocation}${File.separator}${id}")
		if (!wiDir.exists()) {
			wiDir.mkdir()
		}
		File cacheData = new File("${this.cacheLocation}${File.separator}${id}${File.separator}${type}.json");
		def w  = cacheData.newDataOutputStream()
		w << new JsonBuilder(data).toPrettyString()
		w.close()

	}

	/**
	 * Check cache for work item state.
	 *
	 * @param id
	 * @return
	 */
	def getFromCache(def id, String type) {
		File cacheData = new File("${this.cacheLocation}${File.separator}${id}${File.separator}${type}.json");
		if (cacheData.exists()) {
			JsonSlurper s = new JsonSlurper()
			return s.parse(cacheData)
		}
		return null

	}

}
