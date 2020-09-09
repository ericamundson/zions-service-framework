package com.zions.vsts.services.work;

import groovy.util.logging.Slf4j

/**
 * Handles setting up batch of work item changes to ADO.
 * 
 * @author z091182
 *
 */
@Slf4j
class ChangeListManager {
	def changeList = []
	def idMap = [:]
	def count = 0
	WorkManagementService workManagementService
	String collection
	ChangeListManager(String collection, WorkManagementService workManagementService) {
		this.workManagementService = workManagementService
		this.collection = collection
	}

	def add(String key, def item) {
		if (count == 200) {
			flush()
		}
		changeList.push(item)
		idMap[count] = key
		count++
	}

	def flush() {
		if (count == 0) return;
		log.info("Flushing ChangeListManager")
		workManagementService.batchWIChanges(collection, changeList, idMap)
		log.info("Flushed ChangeListManager")
		changeList = []
		idMap = [:]
		count = 0
	}
	
	def size() {
		return count
	}
}