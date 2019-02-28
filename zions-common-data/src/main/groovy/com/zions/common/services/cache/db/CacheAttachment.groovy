package com.zions.common.services.cache.db

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

import groovy.transform.Canonical

@Document
@Canonical
class CacheAttachment {
	@Id
	String id
	
	String project
	
	String key
	
	def binary
}
