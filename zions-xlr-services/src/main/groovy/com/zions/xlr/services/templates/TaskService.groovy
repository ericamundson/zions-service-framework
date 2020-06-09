package com.zions.xlr.services.templates;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zions.xlr.services.rest.client.XlrGenericRestClient;
import com.zions.xlr.services.query.ReleaseQueryService
import com.zions.xlr.services.items.ReleaseItemService
import groovyx.net.http.ContentType

@Component
public class TaskService {
	
	@Autowired
	XlrGenericRestClient xlrGenericRestClient
	
	@Autowired
	ReleaseItemService releaseItemService
	
	public TaskService() {}
	
	def addTask(String containerId, def taskData, int position = -1) {
		def result = null
		if (position != -1) {
			result = xlrGenericRestClient.post(
				requestContentType: ContentType.JSON,
				contentType: ContentType.JSON,
				uri:  "${xlrGenericRestClient.xlrUrl}/api/v1/tasks/${containerId}/tasks",
				body: taskData,
				query:[position: position]
			)
		} else {
			result = xlrGenericRestClient.post(
				requestContentType: ContentType.JSON,
				contentType: ContentType.JSON,
				uri:  "${xlrGenericRestClient.xlrUrl}/api/v1/tasks/${containerId}/tasks",
				body: taskData
			)


		}
	}
}
