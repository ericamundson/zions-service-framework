package com.zions.common.services.cache.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Component

@Component
interface CacheItemRepository extends MongoRepository<CacheItem, String> {
	
	@Query("{ 'project': ?0, module: ?1, 'key': ?2, 'type': ?3}")
	CacheItem findByProjectAndModuleAndKeyAndType(String project, String module, String key, String type);
	
	@Query("{ 'project': ?0, 'module', ?1, 'key': ?2}")
	List<CacheItem> findByProjectAndModuleAndKey(String project, String module, String key);

	@Query("{ 'project': ?0, 'module': ?1, 'type': ?2}")
	List<CacheItem> findByProjectAndModuleAndType(String project, String module, String type);

	List<CacheItem> deleteByProject(String project);
	
	List<CacheItem> deleteByProjectAndModuleAndKey(String project, String module, String key);
	
	Long deleteCacheItemByProject(String project);
}
