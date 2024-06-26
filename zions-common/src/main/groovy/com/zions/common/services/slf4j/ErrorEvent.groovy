package com.zions.common.services.slf4j

import groovy.transform.Canonical

@Canonical
class ErrorEvent  {
	String eventType = 'log.error'
	
	String collectionName 
	
	LogEntity logEntity
}
