package com.zions.xlr.services.events.db

import groovy.transform.Canonical
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document

@Document
@Canonical
//@CompoundIndex(def = "{'releaseId':1}", name = "compound_index_1")
class XlrReleaseSubscription {
	//	[ activityType: eventData.activityType, eventTime: eventData.eventTime, id: eventData.id, message: eventData.message, releaseId: eventData.releaseId, username: eventData.username ]
	
	@Indexed
	String releaseId
	
	@Id
	String id
		
	String pipelineId
	
	String adoProject
	
	boolean isReleasePipeline
		
}
