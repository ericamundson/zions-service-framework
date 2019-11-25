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
	
	/* (non-Javadoc)
	 * @see com.zions.common.services.cache.ICacheManagementService#saveBinaryAsAttachment(java.io.ByteArrayInputStream, java.lang.String, java.lang.String)
	 */
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
			//added these logs to the error catch because there is something breaking in the upload
			log.warn("Filenotfound exception for upload attempt, name: ${name} | id: ${id}", e)
		} catch (IOException e) {
			//somehow, upload step after this receives a blank file.
			log.warn("IOException for upload attempt, name: ${name} | id: ${id}", e)
		}
	
	}

	
	/* (non-Javadoc)
	 * @see com.zions.common.services.cache.ICacheManagementService#saveToCache(java.lang.Object, java.lang.String, java.lang.String)
	 */
	public def saveToCache(def data, String id, String type, Closure c = null) {
		if (c) {
			def currentItem = getFromCache(id, type)
			c(currentItem)
		}
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
		
		if (c) {
			def currentItem = getFromCache(id, type)
			c(currentItem)
		}
	}

	/**
	 * Check cache for work item state.
	 *
	 * @param id
	 * @return
	 */
	def getFromCache(def id, String type) {
		//log.trace("Retrieving id from cache: ${id}")
		File cacheData = new File("${this.cacheLocation}${File.separator}${cacheModule}${File.separator}${id}${File.separator}${type}.json");
		if (cacheData.exists()) {
			//log.trace("Returning cache data id ${id} module ${cacheModule}")
			JsonSlurper s = new JsonSlurper()
			return s.parse(cacheData)
		}
		//log.debug("Did not find id: ${id}")
		return null

	}

	/**
	 * Check cache for work item state.
	 *
	 * @param id
	 * @return
	 */
	def getFromCache(def id, String module, String type) {
		//log.trace("Retrieving id from cache: ${id}")
		File cacheData = new File("${this.cacheLocation}${File.separator}${module}${File.separator}${id}${File.separator}${type}.json");
		if (cacheData.exists()) {
			//log.trace("Returning cache data with id ${id} module ${module}")
			JsonSlurper s = new JsonSlurper()
			return s.parse(cacheData)
		}
		return null

	}
	
	/* (non-Javadoc)
	 * @see com.zions.common.services.cache.ICacheManagementService#clear()
	 */
	@Override
	public void clear() {
		File file = new File("${this.cacheLocation}${File.separator}${cacheModule}")
		file.deleteDir();
		
	}
	
	/* (non-Javadoc)
	 * @see com.zions.common.services.cache.ICacheManagementService#getAllOfType(java.lang.String)
	 */
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

	/* (non-Javadoc)
	 * @see com.zions.common.services.cache.ICacheManagementService#exists(java.lang.Object)
	 */
	@Override
	public boolean exists(Object id) {
		File cacheItem = new File("${this.cacheLocation}${File.separator}${cacheModule}${File.separator}${id}");

		return cacheItem.exists();
	}


	/* (non-Javadoc)
	 * @see com.zions.common.services.cache.ICacheManagementService#deleteById(java.lang.String)
	 */
	void deleteById(String id) {
		if (exists(id)) {
			File cacheItem = new File("${this.cacheLocation}${File.separator}${cacheModule}${File.separator}${id}");
			cacheItem.deleteDir();
		}
	}

	/* (non-Javadoc)
	 * @see com.zions.common.services.cache.ICacheManagementService#deleteByType(java.lang.String)
	 */
	@Override
	public void deleteByType(String type, Closure c = null) {
		if (c) {
			def items = getAllOfType(type)
			items.each { item ->
				c(item)
			}
		}
		File cDir = new File("${this.cacheLocation}${File.separator}${cacheModule}")
		if (cDir.exists()) {
			cDir.eachFileRecurse(FileType.FILES) { File file ->
				String name = file.name
				String cname = "${type}.json" 
				if (name.startsWith(cname)) {
					file.delete()
				}
			}
		}
		
	}

	@Override
	public void deleteByIdAndByType(String id, String type) {
		if (exists(id)) {
			File cacheItem = new File("${this.cacheLocation}${File.separator}${cacheModule}${File.separator}${id}");
			cacheItem.eachFileRecurse(FileType.FILES) { File file ->
				String name = file.name
				String cname = "${type}.json" 
				if (name.startsWith(cname)) {
					file.delete()
				}
			}
		}
		
	}
}
