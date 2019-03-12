package com.zions.common.services.cache.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Component

@Component
interface CacheItemRepository extends MongoRepository<CacheItem, String> {
	
	@Query("{ 'project': ?0, 'key': ?1, 'type': ?2}")
	CacheItem findByProjectAndKeyAndType(String project, String key, String type);
	
	@Query("{ 'project': ?0, 'key': ?1}")
	List<CacheItem> findByProjectAndKey(String project, String key);

	@Query("{ 'project': ?0, 'type': ?1}")
	List<CacheItem> findByProjectAndType(String project, String type);

	List<CacheItem> deleteByProject(String project);
	
	List<CacheItem> deleteByProjectAndKey(String project, key);
	
	Long deleteCacheItemByProject(String project);
}
