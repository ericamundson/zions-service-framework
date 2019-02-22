package com.zions.vsts.services.work;

class ChangeListManager {
	def changeList = []
	def idMap = [:]
	def count = 0
	WorkManagementService workManagementService
	String collection
	String project
	ChangeListManager(String collection, String project, WorkManagementService workManagementService) {
		this.workManagementService = workManagementService
		this.collection = collection
		this.project = project
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
		workManagementService.batchWIChanges(collection, project, changeList, idMap)
		changeList = []
		idMap = [:]
		count = 0
	}
}