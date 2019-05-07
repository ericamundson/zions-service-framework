package com.zions.common.services.cache.db

import groovy.transform.Canonical
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document

@Document
@Canonical
@CompoundIndex(def = "{'project':1, 'module':1, 'key':1, 'type':1}", name = "compound_index_1")
class CacheItem {
	@Id
	String id
	
	String project
	
	String module
	
	String key
	
	String type
	
	String json
	
}
