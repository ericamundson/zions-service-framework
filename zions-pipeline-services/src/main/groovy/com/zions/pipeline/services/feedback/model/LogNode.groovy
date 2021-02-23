package com.zions.pipeline.services.feedback.model;

import groovy.transform.Canonical;
import com.zions.pipeline.services.db.PipelineLogItem

@Canonical
public class LogNode {
	String log
	List<LogNode> nodes = []
	List<PipelineLogItem> logItems = []
	
}
