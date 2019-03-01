package com.zions.common.services.restart

import groovy.transform.Canonical

@Canonical
class Checkpoint {
	String phase
	String pageUrl
	int checkpointId
	def logEntries = []
	String timeStamp
}
