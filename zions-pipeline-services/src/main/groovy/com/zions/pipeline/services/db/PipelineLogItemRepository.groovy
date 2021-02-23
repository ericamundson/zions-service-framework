package com.zions.pipeline.services.db
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Component

import com.zions.common.services.cache.db.CacheItem

@Component
interface PipelineLogItemRepository extends MongoRepository<PipelineLogItem, String> {
	
	@Query("{ 'pipelineId': ?0}")
	List<PipelineLogItem> findByPipelineId(String pipelineId);
}
