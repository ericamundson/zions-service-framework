package com.zions.pipeline.services.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Component

@Component
interface PullRequestCompletedRepository extends MongoRepository<PullRequestCompleted, String> {
	
	@Query("{ 'pullRequestId': ?0}")
	PullRequestCompleted findByPullRequestId(String pullRequestId);
	
	
	
}
