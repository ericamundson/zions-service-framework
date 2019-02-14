package com.zions.common.services.cache.db

import groovy.transform.Canonical
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
@Canonical
class CacheItem {
	@Id
	String id
	
	String project
	
	String key
	
	String type
	
	String json
	
}
