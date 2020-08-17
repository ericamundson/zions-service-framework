package com.zions.xlr.services.events.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Component

@Component
interface XlrReleaseSubscriptionRepository extends MongoRepository<XlrReleaseSubscription, String> {
	
	@Query("{ 'releaseId': ?0}")
	XlrReleaseSubscription findByReleaseId(String releaseId);
	
	
	
}
