package com.zions.common.services.restart

interface ICheckpointManagementService {
	def addCheckpoint(String phase, String pageUrl)
	def addLogentry(String entry)
	Checkpoint getCurrentCheckpoint() 
	Checkpoint selectCheckpoint(String key)
}
