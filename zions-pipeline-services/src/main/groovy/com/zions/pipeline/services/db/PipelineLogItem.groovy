package com.zions.pipeline.services.db

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

import groovy.transform.Canonical

@Document
@Canonical
class PipelineLogItem {
	String timestamp
	String pipelineId
	String log
	List<String> contexts
	String logType
	@Id
	String id
}
