package com.zions.common.services.cache

import groovy.io.FileType
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * File system store for cached ADO items.
 * 
 * @author z091182
 *
 */
@Slf4j
@Component
class CacheManagementService implements ICacheManagementService {
	
	
	//@Autowired
	@Value('${cache.location:cache}')
	String cacheLocation
	
	@Value('${cache.module:CCM}')
	String cacheModule
	
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
			File mDir = new File("${this.cacheLocation}${File.separator}${cacheModule}")
			if (!mDir.exists()) {
				mDir.mkdir()
			}
	
			File wiDir = new File("${this.cacheLocation}${File.separator}${cacheModule}${File.separator}${id}")
			if (!wiDir.exists()) {
				wiDir.mkdir()
			}
			File attachmentDir = new File("${this.cacheLocation}${File.separator}${cacheModule}${File.separator}${id}${File.separator}attachments")
			if (!attachmentDir.exists()) {
				attachmentDir.mkdir()
			}
			File save = new File("${this.cacheLocation}${File.separator}${cacheModule}${File.separator}${id}${File.separator}attachments${File.separator}${name}");
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
		File mDir = new File("${this.cacheLocation}${File.separator}${cacheModule}")
		if (!mDir.exists()) {
			mDir.mkdir()
		}
		File wiDir = new File("${this.cacheLocation}${File.separator}${cacheModule}${File.separator}${id}")
		if (!wiDir.exists()) {
			wiDir.mkdir()
		}
		File cacheData = new File("${this.cacheLocation}${File.separator}${cacheModule}${File.separator}${id}${File.separator}${type}.json");
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
		log.debug("Retrieving id from cache: ${id}")
		File cacheData = new File("${this.cacheLocation}${File.separator}${cacheModule}${File.separator}${id}${File.separator}${type}.json");
		if (cacheData.exists()) {
			log.debug("Returning cache data id ${id} module ${cacheModule}")
			JsonSlurper s = new JsonSlurper()
			return s.parse(cacheData)
		}
		log.debug("Did not find id: ${id}")
		return null

	}

	/**
	 * Check cache for work item state.
	 *
	 * @param id
	 * @return
	 */
	def getFromCache(def id, String module, String type) {
		log.debug("Retrieving id from cache: ${id}")
		File cacheData = new File("${this.cacheLocation}${File.separator}${module}${File.separator}${id}${File.separator}${type}.json");
		if (cacheData.exists()) {
			log.debug("Returning cache data with id ${id} module ${module}")
			JsonSlurper s = new JsonSlurper()
			return s.parse(cacheData)
		}
		return null

	}
	
	@Override
	public void clear() {
		File file = new File("${this.cacheLocation}${File.separator}${cacheModule}")
		file.deleteDir();
		
	}
	
	public def getAllOfType(String type) {
		File cDir = new File("${this.cacheLocation}${File.separator}${cacheModule}")
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
		File cacheItem = new File("${this.cacheLocation}${File.separator}${cacheModule}${File.separator}${id}");

		return cacheItem.exists();
	}


	void deleteById(String id) {
		if (exists(id)) {
			File cacheItem = new File("${this.cacheLocation}${File.separator}${cacheModule}${File.separator}${id}");
			cacheItem.deleteDir();
		}
	}

	@Override
	public void deleteByType(String type) {
		File cDir = new File("${this.cacheLocation}${File.separator}${cacheModule}")
		cDir.eachFileRecurse(FileType.FILES) { File file ->
			String name = file.name
			String cname = "${type}.json" 
			if (name.startsWith(cname)) {
				file.delete()
			}
		}
		
	}
}
