package com.zions.common.services.cache.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Component

@Component
interface CacheItemRepository extends MongoRepository<CacheItem, String> {
	
	@Query("{ 'project': ?0, module: ?1, 'key': ?2, 'type': ?3}")
	CacheItem findByProjectAndModuleAndKeyAndType(String project, String module, String key, String type);
	
	@Query("{ 'project': ?0, 'module': ?1, 'key': ?2}")
	List<CacheItem> findByProjectAndModuleAndKey(String project, String module, String key);

	@Query("{ 'project': ?0, 'module': ?1, 'type': ?2}")
	List<CacheItem> findByProjectAndModuleAndType(String project, String module, String type);

	@Query("{ 'project': ?0, 'module': ?1, 'type': ?2}")
	Page findByProjectAndModuleAndType(String project, String module, String type, Pageable page);

	@Query("{ 'project': ?0, 'type': ?1}")
	List<CacheItem> findByProjectAndType(String project, String type);
	
	List<CacheItem> deleteByProjectAndModule(String project, String module);
	
	List<CacheItem> deleteByProjectAndModuleAndKey(String project, String module, String key);

	List<CacheItem> deleteByProjectAndModuleAndKeyAndType(String project, String module, String key, String type);
	
	Long deleteCacheItemByProjectAndModule(String project, String module);
	
	Long deleteCacheItemByProjectAndModuleAndType(String project, String module, String type);
	Long deleteCacheItemByProjectAndModuleAndKeyAndType(String project, String module, String key, String type);
}
