package com.zions.common.services.cache

import com.zions.common.services.cache.db.CacheItem
import com.zions.common.services.cache.db.CacheItemRepository
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

/**
 * MongoDB store for ADO cached items.
 * 
 * @author z091182
 *
 */
class MongoDBCacheManagementService implements ICacheManagementService {
	@Autowired(required=false)
	MongoTemplate mongoTemplate

	@Autowired(required=false)
	CacheItemRepository repository
	
	
	@Autowired
	@Value('${db.project:coredev}')
	String dbProject
	
	@Value('${cache.location:cache}')
	String cacheLocation
	
	public MongoDBCacheManagementService() {
		
	}


	@Override
	public Object getFromCache(Object id, String type) {
		// TODO Auto-generated method stub
		CacheItem ci = repository.findByProjectAndKeyAndType(dbProject, id, type)
		if (ci == null) return null
		String json = ci.json
		def jMap = new JsonSlurper().parseText(json)
		return jMap
	}

	@Override
	public Object saveToCache(Object data, String id, String type) {
		// TODO Auto-generated method stubC
		CacheItem ci = repository.findByProjectAndKeyAndType(dbProject, id, type)
		String json = new JsonBuilder(data).toPrettyString()
		if (ci == null) {
			ci = new CacheItem([project:dbProject, key:id, type:type, json: json])
		} else {
			ci.json = json;
		}
		return repository.save(ci)
	}

	@Override
	public Object saveBinaryAsAttachment(ByteArrayInputStream result, String name, String id) {
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

	@Override
	public void clear() {
//		Criteria c = Criteria.where('project').is(dbProject)
//		mongoTemplate.remove(new Query(c), CacheItem.class)
		repository.deleteCacheItemByProject(dbProject)

	}

	@Override
	public boolean exists(Object key) {
		List<CacheItem> items = repository.findByProjectAndKey(dbProject, key)
		return (items != null && items.size() > 0)
	}
	
	void deleteById(String id) {
		List<CacheItem> items = repository.deleteByProjectAndKey(dbProject, id)
	
	}


	@Override
	public Object getAllOfType(String type) {
		def wis = [:]
		List<CacheItem> items = repository.findByProjectAndType(dbProject, type)
		
		items.each { CacheItem item -> 
			def wi = new JsonSlurper().parseText(item.json)
			wis[item.key] = wi
		}
		return wis;
	}


}
