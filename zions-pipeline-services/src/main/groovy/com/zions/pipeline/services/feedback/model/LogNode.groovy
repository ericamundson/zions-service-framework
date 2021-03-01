package com.zions.pipeline.services.feedback.model;

import groovy.transform.Canonical;
import com.zions.pipeline.services.db.PipelineLogItem

@Canonical
public class LogNode {
	String name
	List<LogNode> nodes = []
	List<PipelineLogItem> logItems = []
	
}
