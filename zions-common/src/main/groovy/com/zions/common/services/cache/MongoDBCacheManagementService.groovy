package com.zions.common.services.cache

import com.zions.common.services.cache.db.CacheItem
import com.zions.common.services.cache.db.CacheItemRepository
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component

/**
 * MongoDB store for ADO cached items.
 * 
 * @author z091182
 *
 */
@Component
@Slf4j
class MongoDBCacheManagementService implements ICacheManagementService {
	/**
	 * Mongodb setup configuration
	 */
	@Autowired(required=false)
	MongoTemplate mongoTemplate
	
	private static int PAGE_SIZE = 200

	/**
	 * Query controller for cache items being managed
	 */
	@Autowired(required=false)
	CacheItemRepository repository
	
	@Value('${db.project:coredev}')
	String dbProject
	
	@Value('${cache.location:cache}')
	String cacheLocation
	
	@Value('${cache.module:CCM}')
	String cacheModule

	public MongoDBCacheManagementService() {
		
	}

	@Override
	public Object getFromCache(Object id, String type) {
		
		CacheItem ci = repository.findByProjectAndModuleAndKeyAndType(dbProject, cacheModule, id, type)
		if (ci == null) return null
		String json = ci.json
		def jMap = new JsonSlurper().parseText(json)
		return jMap
	}
	
	@Override
	public Object getFromCache(Object id, String module, String type) {
		
		CacheItem ci = repository.findByProjectAndModuleAndKeyAndType(dbProject, module, id, type)
		if (ci == null) return null
		String json = ci.json
		def jMap = new JsonSlurper().parseText(json)
		return jMap
	}

	@Override
	public Object saveToCache(Object data, String id, String type) {
		CacheItem ci = repository.findByProjectAndModuleAndKeyAndType(dbProject, cacheModule, id, type)
		String json = new JsonBuilder(data).toPrettyString()
		if (ci == null) {
			ci = new CacheItem([project:dbProject, module: cacheModule, key:id, type:type, json: json])
		} else {
			ci.json = json;
		}
		return repository.save(ci)
	}

	@Override
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
			log.error(e.getMessage())
		} catch (IOException e) {
			log.error(e.getMessage())
		} catch (NullPointerException e) {
			// weird, this happened to artifact 74184
			log.error(e.getMessage())
		}
		
	
	}

	@Override
	public void clear() {
//		Criteria c = Criteria.where('project').is(dbProject)
//		mongoTemplate.remove(new Query(c), CacheItem.class)
		repository.deleteCacheItemByProjectAndModule(dbProject, cacheModule)

	}

	@Override
	public boolean exists(Object key) {
		List<CacheItem> items = repository.findByProjectAndModuleAndKey(dbProject, cacheModule, key)
		return (items != null && items.size() > 0)
	}
	
	void deleteById(String id) {
		List<CacheItem> items = repository.deleteByProjectAndModuleAndKey(dbProject, cacheModule, id)
	
	}


	//@Override
	public Object getAllOfType(String type, int page = -1) {
		def wis = [:]
		if (page == -1) {
			List<CacheItem> items = repository.findByProjectAndModuleAndType(dbProject, cacheModule, type)
			items.each { CacheItem item -> 
				def wi = new JsonSlurper().parseText(item.json)
				wis[item.key] = wi
			}
			return wis
		}
		
		try {
			PageRequest r = PageRequest.of(page, PAGE_SIZE)
			Page<CacheItem> p = repository.findByProjectAndModuleAndType(dbProject, cacheModule, type, r)
			if (p.content && p.content.size() > 0) {
				p.content.each { CacheItem item ->
					def wi = new JsonSlurper().parseText(item.json)
					wis[item.key] = wi
				}
			} else {
				return wis
			}
		} catch (e) {
			log.warn("No more pages!")
		}
		
		
		return wis;
	}

	public Map getNoneModuleAllOfType(String type) {
		def wis = [:]
		List<CacheItem> items = repository.findByProjectAndType(dbProject, type)
		
		items.each { CacheItem item ->
			def wi = new JsonSlurper().parseText(item.json)
			wis[item.key] = wi
		}
		return wis;
	}

	@Override
	public void deleteByType(String type) {
		Long i = repository.deleteCacheItemByProjectAndModuleAndType(dbProject, cacheModule, type)
		
	}


}
