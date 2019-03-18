package com.zions.common.services.cache

import groovy.io.FileType
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * File system store for cached ADO items.
 * 
 * @author z091182
 *
 */
@Component
class CacheManagementService implements ICacheManagementService {
	
	
	//@Autowired
	@Value('${cache.location:cache}')
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

	@Override
	public void clear() {
		File file = new File(this.cacheLocation)
		file.deleteDir();
		
	}
	
	public def getAllOfType(String type) {
		File cDir = new File(cacheLocation)
		def wis = [:]
		cDir.eachFileRecurse(FileType.FILES) { File file ->
			String name = file.name
			if (name.startsWith(type)) {
				File p = file.parentFile
				String key = p.name
				def wi = new JsonSlurper().parse(file)
				wis[key] = wi
			}
		}
		return wis
	}

	@Override
	public boolean exists(Object id) {
		File cacheItem = new File("${this.cacheLocation}${File.separator}${id}");

		return cacheItem.exists();
	}


	void deleteById(String id) {
		if (exists(id)) {
			File cacheItem = new File("${this.cacheLocation}${File.separator}${id}");
			cacheItem.deleteDir();
		}
	}
}
